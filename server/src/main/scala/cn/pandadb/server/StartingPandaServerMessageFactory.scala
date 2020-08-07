package cn.pandadb.server

import cn.pandadb.database.PandaDB

class StartingPandaServerMessageFactory extends org.neo4j.server.configuration.StartingMessageFactory {
  override def getMessage: String = "======== PandaDB (+Neo4j-3.5.6-BLOB) ======== " + "\r\n" + PandaServer.logo
}
