package cn.pandadb.database

import java.io._

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import cn.pandadb.commons.blob._
import cn.pandadb.commons.blob.storage.BlobStorage
import cn.pandadb.database.blob.BlobIO._
import cn.pandadb.commons.util.ConfigurationUtils._
import cn.pandadb.commons.util.{Configuration, Logging}
import cn.pandadb.database.blob.DefaultBlobFunctions
import cn.pandadb.database.blob.extensions.RuntimeContext
import cn.pandadb.database.cypherplus.CypherPluginRegistry
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.lifecycle.Lifecycle
import org.springframework.context.support.FileSystemXmlApplicationContext

import scala.collection.mutable

/**
  * Created by bluejoe on 2018/11/29.
  */
class BlobPropertyStoreService(storeDir: File, conf: Config, proceduresService: Procedures)
  extends Lifecycle with Logging {
  val runtimeContext: RuntimeContext = conf.asInstanceOf[RuntimeContext];
  val blobIdFactory = BlobIdFactory.get

  conf.asInstanceOf[RuntimeContext].contextPut[BlobPropertyStoreService](this);
  val configuration: Configuration = wrapNeo4jConf(conf);

  val blobStorage: BlobStorage = BlobStorage.create(configuration);

  private var _blobServer: TransactionalBlobStreamServer = _;

  val (valueMatcher, customPropertyProvider) = {
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

      logger.info(s"loading plugins: $path");
      val appctx = new FileSystemXmlApplicationContext("file:" + path);
      appctx.getBean[CypherPluginRegistry](classOf[CypherPluginRegistry]);
    }).getOrElse(new CypherPluginRegistry());

    (cypherPluginRegistry.createValueComparatorRegistry(configuration),
      cypherPluginRegistry.createCustomPropertyProvider(configuration));
  }

  override def shutdown(): Unit = {
  }

  override def init(): Unit = {
  }

  override def stop(): Unit = {
    if (_blobServer != null) {
      _blobServer.shutdown();
    }

    blobStorage.disconnect();
    logger.info(s"blob storage shutdown: $blobStorage");
  }

  private def startBlobServerIfNeeded(): Unit = {
    _blobServer = if (!conf.enabledBoltConnectors().isEmpty) {
      val httpPort = configuration.getValueAsInt("blob.http.port", 1224);
      val servletPath = configuration.getValueAsString("blob.http.servletPath", "/blob");
      val blobServer = new TransactionalBlobStreamServer(this.conf, httpPort, servletPath);
      //set url
      val hostName = configuration.getValueAsString("blob.http.host", "localhost");
      val httpUrl = s"http://$hostName:$httpPort$servletPath";

      conf.asInstanceOf[RuntimeContext].contextPut("blob.server.connector.url", httpUrl);
      blobServer.start();
      blobServer;
    }
    else {
      null;
    }
  }

  override def start(): Unit = {
    blobStorage.initialize(storeDir, blobIdFactory, configuration);
    logger.info(s"instant blob storage initialized: ${blobStorage}");

    //use getRuntimeContext[BlobPropertyStoreService]
    //config.asInstanceOf[RuntimeContextHolder].putRuntimeContext[InstantBlobStorage](_instantStorage);
    registerProcedure(classOf[DefaultBlobFunctions]);
    startBlobServerIfNeeded();
  }

  private def registerProcedure(procedures: Class[_]*) {
    for (procedure <- procedures) {
      proceduresService.registerProcedure(procedure);
      proceduresService.registerFunction(procedure);
    }
  }
}

class BlobCacheInSession(streamServer: TransactionalBlobStreamServer) extends Logging {
  def start() {
    new Thread(new Runnable {
      override def run() {
        while (true) {
          val now = System.currentTimeMillis();
          val ids = cache.filter(_._2._2 < now).map(_._1)
          if (!ids.isEmpty) {
            //logger.debug(s"cached blobs expired: [${ids.mkString(",")}]");
            invalidate(ids);
          }

          Thread.sleep(CHECK_INTERVAL);
        }
      }
    }).start();
  }

  val CHECK_INTERVAL = 10000L;
  val EXPIRATION = 600000L;
  val cache = mutable.Map[String, (Blob, Long)]();

  def put(key: BlobId, blob: Blob): Unit = {
    val s = key.asLiteralString();
    cache(s) = blob -> (System.currentTimeMillis() + EXPIRATION);

    ThreadBoundContext.streamingBlobs.addBlob(s);
  }

  def invalidate(ids: Iterable[String]) = {
    //logger.debug(s"invalidating [${ids.mkString(",")}]");
    cache --= ids;
  }

  def get(key: BlobId): Option[Blob] = cache.get(key.asLiteralString()).map(_._1);

  def get(key: String): Option[Blob] = cache.get(key).map(_._1);
}

//TODO: reuse BOLT session
class TransactionalBlobStreamServer(conf: Config, httpPort: Int, servletPath: String) extends Logging {
  var _server: Server = _;
  val blobCache: BlobCacheInSession =
    conf.asInstanceOf[RuntimeContext].contextPut[BlobCacheInSession](new BlobCacheInSession(this));

  def start(): Unit = {
    _server = new Server(httpPort);
    val blobStreamServlet = new StreamServlet();
    val context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    _server.setHandler(context);
    //add servlet
    context.addServlet(new ServletHolder(blobStreamServlet), servletPath);
    _server.start();
    blobCache.start();

    logger.info(s"blob server started on http://localhost:$httpPort$servletPath");
  }

  def shutdown(): Unit = {
    _server.stop();
  }

  class StreamServlet extends HttpServlet {
    override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val blobId = req.getParameter("bid");
      val opt = blobCache.get(blobId);
      if (opt.isDefined) {
        resp.setContentType(opt.get.mimeType.text);
        resp.setContentLength(opt.get.length.toInt);
        opt.get.offerStream(IOUtils.copy(_, resp.getOutputStream));
      }
      else {
        resp.sendError(500, s"invalid blob id: $blobId");
      }
    }
  }

}