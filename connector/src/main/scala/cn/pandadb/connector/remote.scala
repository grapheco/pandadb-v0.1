package cn.pandadb.connector

import org.neo4j.blob.util.Logging
import org.neo4j.driver.{AuthTokens, GraphDatabase, Record, Session, StatementResult, Transaction, TransactionWork}

import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  * Created by bluejoe on 2019/7/17.
  */
object RemotePandaServer extends Logging {
  def connect(url: String, user: String = "", pass: String = ""): CypherService = {
    new BoltService(url, user, pass);
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