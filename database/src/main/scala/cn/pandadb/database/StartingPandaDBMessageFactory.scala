package cn.pandadb.database

class StartingPandaDBMessageFactory extends org.neo4j.graphdb.factory.StartingMessageFactory {
  override def getMessage: String = "======== PandaDB (+Neo4j-3.5.6-BLOB) ======== " + "\r\n" + PandaDB.logo
}
