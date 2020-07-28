package cn.pandadb.server

import java.io.File
import java.util.Optional

import cn.pandadb.database.{PandaDB, Touchable}
import org.apache.commons.io.IOUtils
import org.neo4j.blob.utils.Logging
import org.neo4j.server.{AbstractNeoServer, CommunityBootstrapper}

import scala.collection.JavaConversions

/**
  * Created by bluejoe on 2019/7/17.
  */
object PandaServer extends Logging with Touchable {
  val logo = IOUtils.toString(this.getClass.getClassLoader.getResourceAsStream("logo.txt"), "utf-8");
  AbstractNeoServer.NEO4J_IS_STARTING_MESSAGE = "======== PandaServer (+Neo4j-3.5.6-BLOB) ======== "+"\r\n"+logo;
  PandaDB.touch;

  def start(dbDir: File, configFile: File, configOverrides: Map[String, String] = Map()): PandaServer = {
    val server = new PandaServer(dbDir, configFile, configOverrides);
    server.start();
    server;
  }
}

class PandaServer(dbDir: File, configFile: File, configOverrides: Map[String, String] = Map()) {
  val server = new CommunityBootstrapper();

  def start(): Int = {
    server.start(dbDir, Optional.of(configFile),
      JavaConversions.mapAsJavaMap(configOverrides));
  }

  def shutdown(): Int = {
    server.stop();
  }
}

object PandaServerStarter {
  def main(args: Array[String]) {
    if (args.length != 2) {
      sys.error(s"Usage:\r\n");
      sys.error(s"\t${this.getClass.getSimpleName} <db-dir> <conf-file>\r\n");
    }
    else {
      PandaServer.start(new File(args(0)),
        new File(args(1)));
    }
  }
}