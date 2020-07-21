package cn.pandadb.database

import java.io.File
import java.util.Optional

import cn.pandadb.commons.util.Logging
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.server.CommunityBootstrapper

import scala.collection.JavaConversions

/**
  * Created by bluejoe on 2018/8/9.
  */
object PidbConnector extends Logging {
  def openDatabase(dbDir: File, propertiesFile: File): GraphDatabaseService = {
    val builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir);
    builder.loadPropertiesFromFile(propertiesFile.getPath);

    logger.info(s"loading configuration from $propertiesFile");
    //bolt server is not required
    builder.setConfig("dbms.connector.bolt.enabled", "false");
    builder.newGraphDatabase();
  }

  //TODO: CypherService over GraphDatabaseService
  def connect(dbs: GraphDatabaseService): CypherService = {
    new LocalGraphService(dbs);
  }

  /**
    * connect remote server
    * @return
    */
  def connect(url: String, user: String = "", pass: String = ""): CypherService = {
    new BoltService(url, user, pass);
  }

  def startServer(dbDir: File, configFile: File, configOverrides: Map[String, String] = Map()): PidbServer = {
    val server = new PidbServer(dbDir, configFile, configOverrides);
    server.start();
    server;
  }
}

class PidbServer(dbDir: File, configFile: File, configOverrides: Map[String, String] = Map()) {
  val server = new CommunityBootstrapper();

  def start(): Unit = {
    server.start(dbDir, Optional.of(configFile),
      JavaConversions.mapAsJavaMap(configOverrides + ("config.file.path" -> configFile.getAbsolutePath)));
  }

  def shutdown(): Unit = {
    server.stop();
  }
}