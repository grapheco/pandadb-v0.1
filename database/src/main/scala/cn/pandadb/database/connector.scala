package cn.pandadb.database

import cn.pandadb.commons.util.Logging
import org.neo4j.driver.internal.types.InternalMapAccessorWithDefaultValue
import org.neo4j.driver.internal.value.{NodeValue, RelationshipValue}
import org.neo4j.driver.internal.{InternalNode, InternalPair, InternalRelationship}
import org.neo4j.driver.v1._
import org.neo4j.driver.v1.summary.ResultSummary
import org.neo4j.driver.v1.util.{Function, Pair}
import org.neo4j.graphdb.{GraphDatabaseService, Result}
import org.neo4j.kernel.impl.core.NodeProxy

import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

trait CypherService extends Logging {
  def queryObjects[T: ClassTag](queryString: String, fnMap: (Record => T)): Iterator[T];

  def execute[T](f: (Session) => T): T;

  def executeQuery[T](queryString: String, fn: (StatementResult => T)): T;

  def executeQuery[T](queryString: String, params: Map[String, AnyRef], fn: (StatementResult => T)): T;

  def executeUpdate(queryString: String);

  def executeUpdate(queryString: String, params: Map[String, AnyRef]);

  final def executeQuery[T](queryString: String, params: Map[String, AnyRef]): Unit =
    executeQuery(queryString, params, (StatementResult) => {
      null.asInstanceOf[T]
    })

  final def querySingleObject[T](queryString: String, fnMap: (Record => T)): T = {
    executeQuery(queryString, (rs: StatementResult) => {
      fnMap(rs.next());
    });
  }

  final def querySingleObject[T](queryString: String, params: Map[String, AnyRef], fnMap: (Record => T)): T = {
    executeQuery(queryString, params, (rs: StatementResult) => {
      fnMap(rs.next());
    });
  }
}

class LocalGraphService(db: GraphDatabaseService)
  extends Logging with CypherService {
  override def queryObjects[T: ClassManifest](queryString: String, fnMap: (Record) => T): Iterator[T] = ???

  override def execute[T](f: (Session) => T): T = {
    throw new UnsupportedOperationException();
  }

  override def executeQuery[T](queryString: String, fn: (StatementResult) => T): T = {
    val tx = db.beginTx();
    val result = db.execute(queryString);
    val t = fn(new StatementResultAdapter(result));
    t;
  }

  override def executeQuery[T](queryString: String, params: Map[String, AnyRef], fn: (StatementResult) => T): T = {
    val tx = db.beginTx();
    val result = db.execute(queryString, params);
    val t = fn(new StatementResultAdapter(result));
    t;
  }

  override def executeUpdate(queryString: String): Unit = {
    val tx = db.beginTx();
    val r = db.execute(queryString);
    r.close();
    tx.success();
  }

  override def executeUpdate(queryString: String, params: Map[String, AnyRef]): Unit = {
    val tx = db.beginTx();
    val r = db.execute(queryString, params);
    r.close();
    tx.success();
  }

  class RecordAdapter(result: Result, raw: Map[String, AnyRef]) extends InternalMapAccessorWithDefaultValue with Record {

    val list: List[(String, Value)] = result.columns().map(x => x -> {
      raw(x) match {
        case m: org.neo4j.graphdb.Node => new NodeValue(new InternalNode(m.getId, m.getLabels.map(_.name()),
          JavaConversions.mapAsJavaMap(m.getAllProperties.map(x =>
            x._1 -> Values.value(x._2)).toMap)))

        case m: org.neo4j.graphdb.Relationship => new RelationshipValue(new InternalRelationship(m.getId, m.getStartNodeId, m.getEndNodeId,
          m.getType.name(),
          JavaConversions.mapAsJavaMap(m.getAllProperties.map(x =>
            x._1 -> Values.value(x._2)).toMap)))

        case m => Values.value(m)
      }
    }
    ).toList

    override def asMap(): java.util.Map[String, AnyRef] = raw

    override def asMap[T](mapper: Function[Value, T]): java.util.Map[String, T] =
      JavaConversions.mapAsJavaMap(
        list.map(x =>
          x._1 -> mapper.apply(x._2)).toMap
      )

    override def values(): java.util.List[Value] = list.map(_._2)

    override def get(key: String): Value = list.find(_._1.equals(key)).get._2

    override def get(index: Int): Value = list(index)._2

    override def size(): Int = raw.size

    override def fields(): java.util.List[Pair[String, Value]] =
      JavaConversions.seqAsJavaList(
        list.map(x =>
          InternalPair.of[String, Value](x._1, x._2))
      )

    override def keys(): java.util.List[String] = result.columns()

    override def containsKey(key: String): Boolean = raw.contains(key)

    override def index(key: String): Int = list.indexWhere(_._1.equals(key))
  }

  class StatementResultAdapter(result: Result) extends StatementResult {
    override def next(): Record = new RecordAdapter(result, JavaConversions.mapAsScalaMap(result.next()).toMap)

    override def consume(): ResultSummary = throw new UnsupportedOperationException()

    override def peek(): Record = throw new UnsupportedOperationException()

    override def summary(): ResultSummary = throw new UnsupportedOperationException()

    override def hasNext: Boolean = result.hasNext

    override def keys(): java.util.List[String] = result.columns()

    override def list(): java.util.List[Record] = this.toList

    override def list[T](mapFunction: Function[Record, T]): java.util.List[T] = list().map(mapFunction.apply(_))

    override def single(): Record =
      if (hasNext)
        null;
      else
        next();
  }

}

class BoltService(url: String, user: String = "", pass: String = "")
  extends Logging with CypherService {

  lazy val _driver = GraphDatabase.driver(url, AuthTokens.basic(user, pass));

  override def execute[T](f: (Session) => T): T = {
    val session = _driver.session();
    val result = f(session);
    session.close();
    result;
  }

  override def queryObjects[T: ClassTag](queryString: String, fnMap: (Record => T)): Iterator[T] = {
    executeQuery(queryString, (result: StatementResult) => {
      result.map(fnMap)
    });
  }

  override def executeUpdate(queryString: String) = {
    _executeUpdate(queryString, None);
  }

  override def executeUpdate(queryString: String, params: Map[String, AnyRef]) = {
    _executeUpdate(queryString, Some(params));
  }

  override def executeQuery[T](queryString: String, fn: (StatementResult => T)): T = {
    _executeQuery(queryString, None, fn);
  }

  override def executeQuery[T](queryString: String, params: Map[String, AnyRef], fn: (StatementResult => T)): T = {
    _executeQuery(queryString, Some(params), fn);
  }

  private def _executeUpdate[T](queryString: String, optParams: Option[Map[String, AnyRef]]): Unit = {
    execute((session: Session) => {
      logger.debug(s"execute update: $queryString");
      session.writeTransaction(new TransactionWork[T] {
        override def execute(tx: Transaction): T = {
          if (optParams.isDefined)
            tx.run(queryString, JavaConversions.mapAsJavaMap(optParams.get));
          else
            tx.run(queryString);

          null.asInstanceOf[T];
        }
      });
    });
  }

  private def _executeQuery[T](queryString: String, optParams: Option[Map[String, AnyRef]], fn: (StatementResult => T)): T = {
    execute((session: Session) => {
      logger.debug(s"execute query: $queryString");
      session.readTransaction(new TransactionWork[T] {
        override def execute(tx: Transaction): T = {
          val result = if (optParams.isDefined)
            tx.run(queryString, JavaConversions.mapAsJavaMap(optParams.get));
          else
            tx.run(queryString);

          fn(result);
        }
      });
    });
  }
}