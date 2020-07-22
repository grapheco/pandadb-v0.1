/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.interpreted.commands.expressions

import cn.pandadb.commons.util.ReflectUtils._
import cn.pandadb.commons.RuntimeContext
import cn.pandadb.query.{BlobFactory, CustomPropertyProvider, ValueMatcher}
import org.neo4j.cypher.internal.frontend.v3_4.parser.{AlgoNameWithThresholdExpr, BlobURL}
import org.neo4j.cypher.internal.runtime.interpreted.commands.predicates.Predicate
import org.neo4j.cypher.internal.runtime.interpreted.commands.values.KeyToken
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.cypher.internal.runtime.interpreted.{ExecutionContext, UpdateCountingQueryContext}
import org.neo4j.values.AnyValue
import org.neo4j.values.storable._
import org.neo4j.values.virtual.VirtualValues

object QueryStateUtils {
  def getInstanceContext(state: QueryState): RuntimeContext = {
    //FIXME: ThreadBoundContext?
    if (state.query.isInstanceOf[UpdateCountingQueryContext])
      state._get("query.inner.inner.transactionalContext.tc.graph.graph.config")
    else
      state._get("query.inner.transactionalContext.tc.graph.graph.config")
  }.asInstanceOf[RuntimeContext]
}

case class BlobLiteralCommand(url: BlobURL) extends Expression {
  def apply(ctx: ExecutionContext, state: QueryState): AnyValue = {
    BlobValue(QueryStateUtils.getInstanceContext(state).contextGet[BlobFactory].createBlob(url));
  }

  override def rewrite(f: (Expression) => Expression): Expression = f(this)

  override def arguments: Seq[Expression] = Nil

  override def symbolTableDependencies: Set[String] = Set()
}

case class CustomPropertyCommand(mapExpr: Expression, propertyKey: KeyToken)
  extends Expression with Product with Serializable {

  def apply(ctx: ExecutionContext, state: QueryState): AnyValue =
    mapExpr(ctx, state) match {
      case n if n == Values.NO_VALUE => Values.NO_VALUE

      case x: Value =>
        val pv = QueryStateUtils.getInstanceContext(state).contextGet[CustomPropertyProvider]
          .getCustomProperty(x.asObject, propertyKey.name)

        pv.map(Values.unsafeOf(_, true)).getOrElse(Values.NO_VALUE)
    }

  def rewrite(f: (Expression) => Expression) = f(CustomPropertyCommand(mapExpr.rewrite(f), propertyKey.rewrite(f)))

  override def children = Seq(mapExpr, propertyKey)

  def arguments = Seq(mapExpr)

  def symbolTableDependencies = mapExpr.symbolTableDependencies

  override def toString = s"$mapExpr.${propertyKey.name}"
}

trait SemanticOperatorSupport {
  val lhsExpr: Expression;
  val ant: Option[AlgoNameWithThresholdExpr];
  val rhsExpr: Expression;

  val (algorithm, threshold) = ant match {
    case None => (None, None)
    case Some(AlgoNameWithThresholdExpr(a, b)) => (a, b)
  }

  def getOperatorString: String

  def rewriteMethod: (Expression, Option[AlgoNameWithThresholdExpr], Expression) => Expression

  def execute[T](m: ExecutionContext, state: QueryState)(f: (AnyValue, AnyValue, RuntimeContext) => T): T = {
    val lValue = lhsExpr(m, state)
    val rValue = rhsExpr(m, state)
    f(lValue, rValue, QueryStateUtils.getInstanceContext(state))
  }

  override def toString: String = lhsExpr.toString() + this.getOperatorString + rhsExpr.toString()

  def containsIsNull = false

  def rewrite(f: (Expression) => Expression) = f(rhsExpr.rewrite(f) match {
    case other => rewriteMethod(lhsExpr.rewrite(f), ant, other)
  })

  def arguments = Seq(lhsExpr, rhsExpr)

  def symbolTableDependencies = lhsExpr.symbolTableDependencies ++ rhsExpr.symbolTableDependencies
}

case class SemanticLikeCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                              (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {
  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    execute(m, state) { (lValue: AnyValue, rValue: AnyValue, ctx: RuntimeContext) =>
      (lValue, rValue) match {
        case (Values.NO_VALUE, Values.NO_VALUE) => Some(true)
        case (_, Values.NO_VALUE) => Some(false)
        case (Values.NO_VALUE, _) => Some(false)
        case (a: Value, b: Value) => ctx.contextGet[ValueMatcher].like(a.asObject(), b.asObject(), algorithm, threshold)
      }
    }
  }

  override def getOperatorString: String = "~:"

  override def rewriteMethod = SemanticLikeCommand(_, _, _)(converter)
}


case class SemanticUnlikeCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                                (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {

  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    new SemanticLikeCommand(lhsExpr, ant, rhsExpr)(converter).isMatch(m, state).map(!_);
  }

  override def getOperatorString: String = "!:"

  override def rewriteMethod = SemanticUnlikeCommand(_, _, _)(converter)
}

case class SemanticContainCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                                 (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {

  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    execute(m, state) { (lValue: AnyValue, rValue: AnyValue, ctx: RuntimeContext) =>
      (lValue, rValue) match {
        case (Values.NO_VALUE, Values.NO_VALUE) => Some(true)
        case (_, Values.NO_VALUE) => Some(false)
        case (Values.NO_VALUE, _) => Some(false)
        case (a: Value, b: Value) => ctx.contextGet[ValueMatcher].containsOne(a.asObject(), b.asObject(), algorithm, threshold)
      }
    }
  }

  override def getOperatorString: String = ">:"

  override def rewriteMethod = SemanticContainCommand(_, _, _)(converter)
}

case class SemanticInCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                            (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {
  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    new SemanticContainCommand(rhsExpr, ant, lhsExpr)(converter).isMatch(m, state)
  }

  override def getOperatorString: String = "<:"

  override def rewriteMethod = SemanticInCommand(_, _, _)(converter)
}

case class SemanticContainSetCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                                    (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {

  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    execute(m, state) { (lValue: AnyValue, rValue: AnyValue, ctx: RuntimeContext) =>
      (lValue, rValue) match {
        case (Values.NO_VALUE, Values.NO_VALUE) => Some(true)
        case (_, Values.NO_VALUE) => Some(false)
        case (Values.NO_VALUE, _) => Some(false)
        case (a: Value, b: Value) => ctx.contextGet[ValueMatcher].containsSet(a.asObject(), b.asObject(), algorithm, threshold)
      }
    }
  }

  override def getOperatorString: String = ">>:"

  override def rewriteMethod = SemanticContainSetCommand(_, _, _)(converter)
}

case class SemanticSetInCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                               (implicit converter: TextValue => TextValue = identity)
  extends Predicate with SemanticOperatorSupport {
  override def isMatch(m: ExecutionContext, state: QueryState): Option[Boolean] = {
    new SemanticContainSetCommand(rhsExpr, ant, lhsExpr)(converter).isMatch(m, state)
  }

  override def getOperatorString: String = "<<:"

  override def rewriteMethod = SemanticSetInCommand(_, _, _)(converter)
}

case class SemanticCompareCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                                 (implicit converter: TextValue => TextValue = identity)
  extends Expression with SemanticOperatorSupport {

  override def apply(ctx: ExecutionContext, state: QueryState): AnyValue = {
    execute(ctx, state) { (lValue: AnyValue, rValue: AnyValue, ctx: RuntimeContext) =>
      (lValue, rValue) match {
        case (x, y) if x == Values.NO_VALUE || y == Values.NO_VALUE => Values.NO_VALUE
        case (a: Value, b: Value) => ctx.contextGet[ValueMatcher].compareOne(a.asObject, b.asObject(), algorithm).
          map(Values.doubleValue(_)).getOrElse(Values.NO_VALUE)
      }
    }
  }

  override def getOperatorString: String = "::"

  override def rewriteMethod = SemanticCompareCommand(_, _, _)(converter)
}

case class SemanticSetCompareCommand(lhsExpr: Expression, ant: Option[AlgoNameWithThresholdExpr], rhsExpr: Expression)
                                    (implicit converter: TextValue => TextValue = identity)
  extends Expression with SemanticOperatorSupport {

  override def apply(ctx: ExecutionContext, state: QueryState): AnyValue = {
    execute(ctx, state) { (lValue: AnyValue, rValue: AnyValue, ctx: RuntimeContext) =>
      (lValue, rValue) match {
        case (x, y) if x == Values.NO_VALUE || y == Values.NO_VALUE => Values.NO_VALUE
        case (a: Value, b: Value) => ctx.contextGet[ValueMatcher].compareSet(a.asObject, b.asObject(), algorithm).
          map {
            aa =>
              VirtualValues.list(aa.map {
                a => VirtualValues.list(a.map(x => Values.doubleValue(x)).toSeq: _*)
              }.toSeq: _*)
          }.getOrElse(Values.NO_VALUE)
      }
    }
  }

  override def getOperatorString: String = ":::"

  override def rewriteMethod = SemanticSetCompareCommand(_, _, _)(converter)
}

class InvalidSemanticOperatorException(compared: AnyValue) extends RuntimeException {

}