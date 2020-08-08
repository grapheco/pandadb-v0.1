package cn.pandadb.connector

import org.neo4j.blob.util.Logging
import org.neo4j.driver.{Record, Session, StatementResult}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * Created by bluejoe on 2019/7/26.
 */
trait CypherService extends Logging {
  final def queryObjects[T: ClassTag](queryString: String, fnMap: (Record => T)): Iterator[T] = {
    executeQuery(queryString, (result) => {
      val records = ArrayBuffer[T]()
      while (result.hasNext) {
        val record = result.next()
        records += fnMap(record)
      }
      records.iterator
    })
  }

  final def queryObjects[T: ClassTag](queryString: String, params: Map[String, AnyRef], fnMap: (Record => T)): Iterator[T] = {
    executeQuery(queryString, params, (result) => {
      val records = ArrayBuffer[T]()
      while (result.hasNext) {
        val record = result.next()
        records += fnMap(record)
      }
      records.iterator
    })
  }

  def execute[T](f: (Session) => T): T;

  def executeQuery[T](queryString: String, fn: (StatementResult => T)): T;

  def executeQuery[T](queryString: String, params: Map[String, AnyRef], fn: (StatementResult => T)): T;

  def executeUpdate(queryString: String);

  def executeUpdate(queryString: String, params: Map[String, AnyRef]);

  final def querySingleObject[T](queryString: String, fnMap: (Record => T)): T = {
    executeQuery(queryString, (rs: StatementResult) => {
      fnMap(rs.next());
    })
  }

  final def querySingleObject[T](queryString: String, params: Map[String, AnyRef], fnMap: (Record => T)): T = {
    executeQuery(queryString, params, (rs: StatementResult) => {
      fnMap(rs.next());
    })
  }
}
