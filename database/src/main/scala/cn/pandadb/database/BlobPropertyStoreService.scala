package cn.pandadb.database

import java.io._

import cn.pandadb.commons.RuntimeContext
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import cn.pandadb.commons.blob._
import cn.pandadb.commons.blob.BlobStorage
import cn.pandadb.commons.util.ConfigurationUtils._
import cn.pandadb.commons.util.{Configuration, Logging}
import cn.pandadb.database.blob.{BlobIO, DefaultBlobFunctions}
import cn.pandadb.query.{BlobFactory, CustomPropertyProvider, ValueMatcher}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.lifecycle.Lifecycle
import org.springframework.context.support.FileSystemXmlApplicationContext

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by bluejoe on 2018/11/29.
 */
class BlobPropertyStoreService(storeDir: File, conf: Config, proceduresService: Procedures)
  extends Lifecycle with Logging {
  val runtimeContext: RuntimeContext = conf.asInstanceOf[RuntimeContext];
  val blobIdFactory = BlobIdFactory.get
  val closables = ArrayBuffer[() => Unit]()
  conf.asInstanceOf[RuntimeContext].contextPut[BlobPropertyStoreService](this);
  val configuration: Configuration = BlobIO.wrapNeo4jConf(conf);

  override def shutdown(): Unit = {
  }

  override def init(): Unit = {
  }

  override def stop(): Unit = {
    closables.foreach(_.apply())
    logger.info(s"stopped BlobPropertyStoreService");
  }

  override def start(): Unit = {
    val blobStorage: BlobStorage = BlobStorage.create(configuration);
    blobStorage.initialize(storeDir, blobIdFactory, configuration);
    closables += { () => blobStorage.disconnect() }
    runtimeContext.contextPut[BlobStorage](blobStorage)

    logger.info(s"instant blob storage initialized: ${blobStorage}")

    val cypherPluginRegistry = configuration.getRaw("blob.plugins.conf").map(x => {
      val path = new File(x) match {
        case f: File if f.isAbsolute =>
          f.getPath
        case f: File =>
          configuration.getRaw("config.file.path").map(m =>
            new File(new File(m).getParentFile, x).getAbsoluteFile.getCanonicalPath).getOrElse(f.getCanonicalPath)
      }

      logger.info(s"loading plugins: $path");
      val appctx = new FileSystemXmlApplicationContext("file:" + path);
      appctx.getBean[CypherPluginRegistry](classOf[CypherPluginRegistry]);
    }).getOrElse(new CypherPluginRegistry());

    val valueMatcher = cypherPluginRegistry.createValueComparatorRegistry(configuration)
    val customPropertyProvider = cypherPluginRegistry.createCustomPropertyProvider(configuration)
    runtimeContext.contextPut[ValueMatcher](valueMatcher)
    runtimeContext.contextPut[CustomPropertyProvider](customPropertyProvider)

    val blobFactory = new SimpleBlobFactory()
    runtimeContext.contextPut[BlobFactory](blobFactory)

    //use getRuntimeContext[BlobPropertyStoreService]
    //config.asInstanceOf[RuntimeContextHolder].putRuntimeContext[InstantBlobStorage](_instantStorage);
    registerProcedure(classOf[DefaultBlobFunctions]);

    if (!conf.enabledBoltConnectors().isEmpty) {
      val httpPort = configuration.getValueAsInt("blob.http.port", 1224);
      val servletPath = configuration.getValueAsString("blob.http.servletPath", "/blob");
      val blobServer = new TransactionalBlobStreamServer(this.conf, httpPort, servletPath);
      //set url
      val hostName = configuration.getValueAsString("blob.http.host", "localhost");
      val httpUrl = s"http://$hostName:$httpPort$servletPath";

      conf.asInstanceOf[RuntimeContext].contextPut("blob.server.connector.url", httpUrl);
      blobServer.start();
      closables += { () => blobServer.shutdown() }
    }
  }

  private def registerProcedure(procedures: Class[_]*) {
    for (procedure <- procedures) {
      proceduresService.registerProcedure(procedure);
      proceduresService.registerFunction(procedure);
    }
  }
}

class BlobCacheInSession(streamServer: TransactionalBlobStreamServer) extends Logging {
  val CHECK_INTERVAL = 10000L;
  val EXPIRATION = 600000L;
  val cache = mutable.Map[String, (Blob, Long)]();

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