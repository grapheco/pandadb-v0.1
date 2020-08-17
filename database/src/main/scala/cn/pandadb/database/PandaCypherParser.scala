package cn.pandadb.database

import org.neo4j.blob.util.Logging
import org.neo4j.cypher.internal.v3_5.expressions
import org.neo4j.cypher.internal.v3_5.parser.CypherParser
import org.parboiled.scala.{Rule1, group}
import org.neo4j.cypher.internal.v3_5.{expressions => ast}
import org.neo4j.cypher.internal.v3_5.expressions._
import org.parboiled.scala._

class PandaCypherParser extends CypherParser with Logging {
  if (logger.isDebugEnabled()) {
    logger.debug(s"using ${this.getClass.getSimpleName}...")
  }

  private def BlobURLPath: Rule1[String] = rule("<blob url path>")(
    push(new java.lang.StringBuilder) ~ zeroOrMore(
      !(RightArrowHead) ~ ANY
        ~:% withContext(appendToStringBuilder(_)(_))
    )
      ~~> (_.toString())
  )

  private def BlobLiteral: Rule1[BlobLiteralExpr] = rule("<blob>")(
    LeftArrowHead ~ ignoreCase("FILE://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobFileURL(x)))
      | LeftArrowHead ~ ignoreCase("BASE64://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobBase64URL(x.mkString(""))))
      | LeftArrowHead ~ ignoreCase("HTTP://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobHttpURL(s"http://${x.mkString("")}")))
      | LeftArrowHead ~ ignoreCase("HTTPS://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobHttpURL(s"https://${x.mkString("")}")))
      | LeftArrowHead ~ ignoreCase("FTP://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobFtpURL(s"ftp://${x.mkString("")}")))
      | LeftArrowHead ~ ignoreCase("SFTP://") ~ BlobURLPath ~ RightArrowHead
      ~~>> (x => BlobLiteralExpr(BlobFtpURL(s"sftp://${x.mkString("")}")))
  )

  override def Expression1: Rule1[ast.Expression] = rule("an expression")(
    NumberLiteral
      | StringLiteral
      | BlobLiteral
      | Parameter
      | keyword("TRUE") ~ push(ast.True()(_))
      | keyword("FALSE") ~ push(ast.False()(_))
      | keyword("NULL") ~ push(ast.Null()(_))
      | CaseExpression
      | group(keyword("COUNT") ~~ "(" ~~ "*" ~~ ")") ~ push(ast.CountStar()(_))
      | MapLiteral
      | MapProjection
      | ListComprehension
      | PatternComprehension
      | group("[" ~~ zeroOrMore(Expression, separator = CommaSep) ~~ "]") ~~>> (ast.ListLiteral(_))
      | group(keyword("FILTER") ~~ "(" ~~ FilterExpression ~~ ")") ~~>> (ast.FilterExpression(_, _, _))
      | group(keyword("EXTRACT") ~~ "(" ~~ FilterExpression ~ optional(WS ~ "|" ~~ Expression) ~~ ")") ~~>> (ast.ExtractExpression(_, _, _, _))
      | group(keyword("REDUCE") ~~ "(" ~~ Variable ~~ "=" ~~ Expression ~~ "," ~~ IdInColl ~~ "|" ~~ Expression ~~ ")") ~~>> (ast.ReduceExpression(_, _, _, _, _))
      | group(keyword("ALL") ~~ "(" ~~ FilterExpression ~~ ")") ~~>> (ast.AllIterablePredicate(_, _, _))
      | group(keyword("ANY") ~~ "(" ~~ FilterExpression ~~ ")") ~~>> (ast.AnyIterablePredicate(_, _, _))
      | group(keyword("NONE") ~~ "(" ~~ FilterExpression ~~ ")") ~~>> (ast.NoneIterablePredicate(_, _, _))
      | group(keyword("SINGLE") ~~ "(" ~~ FilterExpression ~~ ")") ~~>> (ast.SingleIterablePredicate(_, _, _))
      | ShortestPathPattern ~~> expressions.ShortestPathExpression
      | RelationshipsPattern ~~> PatternExpression
      | parenthesizedExpression
      | FunctionInvocation
      | Variable
  )

  private def AlgoNameWithThreshold: Rule1[AlgoNameWithThresholdExpr] = rule("an algorithm with threshold") {
    group(SymbolicNameString ~ optional(operator("/") ~ DoubleLiteral)) ~~>>
      ((a, b) => AlgoNameWithThresholdExpr(Some(a), b.map(_.value))) |
      group(DoubleLiteral ~ optional(operator("/") ~ SymbolicNameString)) ~~>>
        ((a, b) => AlgoNameWithThresholdExpr(b, Some(a.value)))
  }

  private def AlgoName: Rule1[AlgoNameWithThresholdExpr] = rule("an algorithm with threshold") {
    group(SymbolicNameString) ~~>>
      ((a) => AlgoNameWithThresholdExpr(Some(a), None))
  }

  override def Expression3: Rule1[org.neo4j.cypher.internal.v3_5.expressions.Expression] = rule("an expression") {
    Expression2 ~ zeroOrMore(WS ~ (
      group(operator("=~") ~~ Expression2) ~~>> (expressions.RegexMatch(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | group(operator("~:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticLikeExpr(a, b, c))
        | group(operator("!:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticUnlikeExpr(a, b, c))
        | group(operator(":::") ~ optional(AlgoName) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticSetCompareExpr(a, b, c))
        | group(operator(">>:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticContainSetExpr(a, b, c))
        | group(operator("<<:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticSetInExpr(a, b, c))
        | group(operator("::") ~ optional(AlgoName) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticCompareExpr(a, b, c))
        | group(operator(">:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticContainExpr(a, b, c))
        | group(operator("<:") ~ optional(AlgoNameWithThreshold) ~~ Expression2) ~~>>
        ((a: ast.Expression, b, c) =>
          SemanticInExpr(a, b, c))
        | group(keyword("IN") ~~ Expression2) ~~>> (expressions.In(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | group(keyword("STARTS WITH") ~~ Expression2) ~~>> (expressions.StartsWith(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | group(keyword("ENDS WITH") ~~ Expression2) ~~>> (expressions.EndsWith(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | group(keyword("CONTAINS") ~~ Expression2) ~~>> (expressions.Contains(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | keyword("IS NULL") ~~>> (expressions.IsNull(_: org.neo4j.cypher.internal.v3_5.expressions.Expression))
        | keyword("IS NOT NULL") ~~>> (expressions.IsNotNull(_: org.neo4j.cypher.internal.v3_5.expressions.Expression))
      ): ReductionRule1[org.neo4j.cypher.internal.v3_5.expressions.Expression, org.neo4j.cypher.internal.v3_5.expressions.Expression])
  }

  override def Expression2: Rule1[org.neo4j.cypher.internal.v3_5.expressions.Expression] = rule("an expression") {
    Expression1 ~ zeroOrMore(WS ~ (
      PropertyLookup
        | operator("->") ~~ (PropertyKeyName ~~>> (CustomPropertyExpr(_: ast.Expression, _)))
        | NodeLabels ~~>> (ast.HasLabels(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | "[" ~~ Expression ~~ "]" ~~>> (ast.ContainerIndex(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _))
        | "[" ~~ optional(Expression) ~~ ".." ~~ optional(Expression) ~~ "]" ~~>> (ast.ListSlice(_: org.neo4j.cypher.internal.v3_5.expressions.Expression, _, _))
      ))
  }
}
