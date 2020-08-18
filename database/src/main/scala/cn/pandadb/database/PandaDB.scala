package cn.pandadb.database

import java.io.File
import org.neo4j.blob.util.Logging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.facade.extension.{DatabaseLifecyclePlugin, DatabaseLifecyclePluginContext}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.springframework.context.support.FileSystemXmlApplicationContext

/**
 * Created by bluejoe on 2019/7/17.
 */
object PandaDB extends Logging {
  def openDatabase(dbDir: File, propertiesFile: File): GraphDatabaseService = {
    val builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbDir);
    val pfile = propertiesFile.getCanonicalFile.getAbsoluteFile
    logger.info(s"loading configuration from $pfile");
    builder.loadPropertiesFromFile(pfile.getPath);
    //bolt server is not required
    builder.setConfig("dbms.connector.bolt.enabled", "false");
    builder.newGraphDatabase();
  }
}

class SemanticOperatorPlugin extends DatabaseLifecyclePlugin with Logging {
  override def init(ctx: DatabaseLifecyclePluginContext): Unit = {
    val configuration = ctx.configuration;
    val cypherPluginRegistry = new CypherPluginRegistry(configuration)
    val customPropertyProvider = cypherPluginRegistry.createCustomPropertyProvider()
    val valueMatcher = cypherPluginRegistry.createValueComparatorRegistry()

    ctx.instanceContext.put[CustomPropertyProvider](customPropertyProvider)
    ctx.instanceContext.put[ValueMatcher](valueMatcher)
  }

  override def stop(ctx: DatabaseLifecyclePluginContext): Unit = {

  }

  override def start(ctx: DatabaseLifecyclePluginContext): Unit = {

  }
}
