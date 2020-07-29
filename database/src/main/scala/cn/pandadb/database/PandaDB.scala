package cn.pandadb.database

import java.io.File
import org.neo4j.blob.util.Logging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.impl.blob.{BlobPropertyStoreServiceContext, BlobPropertyStoreServicePlugin, BlobPropertyStoreServicePlugins}
import org.springframework.context.support.FileSystemXmlApplicationContext

/**
 * Created by bluejoe on 2019/7/17.
 */
object PandaDB extends Logging with Touchable {
  CypherInjection.touch;
  SemanticOperatorPluginInjection.touch;

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

trait Touchable {
  //do nothing, just ensure this object is initialized
  final def touch: Unit = {
  }
}

object SemanticOperatorPluginInjection extends Touchable {
  BlobPropertyStoreServicePlugins.add(new SemanticOperatorPlugin());
}

class SemanticOperatorPlugin extends BlobPropertyStoreServicePlugin with Logging {
  override def init(ctx: BlobPropertyStoreServiceContext): Unit = {
    val configuration = ctx.configuration;
    val cypherPluginRegistry = configuration.getRaw("blob.plugins.conf").map(x => {
      val xml = new File(x);

      val path =
        if (xml.isAbsolute) {
          xml.getPath
        }
        else {
          val configFilePath = configuration.getRaw("config.file.path")
          if (configFilePath.isDefined) {
            new File(new File(configFilePath.get).getParentFile, x).getAbsoluteFile.getCanonicalPath
          }
          else {
            xml.getAbsoluteFile.getCanonicalPath
          }
        }

      logger.info(s"loading semantic plugins: $path");
      val appctx = new FileSystemXmlApplicationContext("file:" + path);
      appctx.getBean[CypherPluginRegistry](classOf[CypherPluginRegistry]);
    }).getOrElse {
      logger.info(s"semantic plugins not loaded: blob.plugins.conf=null");
      new CypherPluginRegistry()
    }

    val customPropertyProvider = cypherPluginRegistry.createCustomPropertyProvider(configuration);
    val valueMatcher = cypherPluginRegistry.createValueComparatorRegistry(configuration);

    ctx.instanceContext.put[CustomPropertyProvider](customPropertyProvider);
    ctx.instanceContext.put[ValueMatcher](valueMatcher);
  }

  override def stop(ctx: BlobPropertyStoreServiceContext): Unit = {

  }

  override def start(ctx: BlobPropertyStoreServiceContext): Unit = {

  }
}
