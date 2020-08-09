package cn.pandadb.server

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.neo4j.blob.Blob
import org.neo4j.blob.util.Logging
import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.{KernelTransactionEvent, KernelTransactionEventHub, KernelTransactionEventListener, TransactionalBlobCached}
import org.neo4j.server.configuration.ApplicationContextEnhancer
import scala.collection.mutable

class BlobForwardService extends ApplicationContextEnhancer {
  val cache = new BlobCacheForHttpRequest
  KernelTransactionEventHub.addListener(cache)

  override def enhance(handler: ServletContextHandler, config: Config): Unit = {
    handler.addServlet(new ServletHolder(new HttpServlet() {
      override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
        val uri = req.getRequestURI
        val handle = uri.substring(uri.indexOf("/blob/") + "/blob/".length)
        if (handle.isEmpty) {
          resp.sendError(500, s"invalid handle: $handle")
        }

        cache.get(handle).orElse {
          resp.sendError(500, s"invalid handle: $handle")
          scala.None
        }.foreach(blob => {
          resp.setContentLengthLong(blob.length)
          resp.setContentType(blob.mimeType.text)
          blob.offerStream(is =>
            IOUtils.copy(is, resp.getOutputStream)
          )
        })
      }
    }), "/blob/*")
  }
}

class BlobCacheForHttpRequest extends KernelTransactionEventListener with Logging {
  //3m
  val MAX_ALIVE = 3 * 60 * 1000

  //5s
  val CHECK_INTERVAL = 5 * 1000

  case class Entry(id: String, blob: Blob, transactionId: String, time: Long = System.currentTimeMillis()) {
    val expired = time + MAX_ALIVE
  }

  val _blobCache = mutable.Map[String, Entry]();

  //expired blob checking thread
  new Thread(new Runnable() {
    override def run(): Unit = {
      while (true) {
        Thread.sleep(CHECK_INTERVAL);
        if (!_blobCache.isEmpty) {
          val now = System.currentTimeMillis()
          val handles = _blobCache.filter(_._2.expired < now).map(_._1)
          if (!handles.isEmpty) {
            if (logger.isDebugEnabled())
              logger.debug(s"invalidated: ${handles.toList}")
            _blobCache --= handles
          }
        }
      }
    }
  }).start()

  def get(handle: String): Option[Blob] = {
    _blobCache.get(handle).map(_.blob)
  }

  override def notified(event: KernelTransactionEvent): Unit = event match {
    case TransactionalBlobCached(handle: String, blob: Blob, transactionId: String) =>
      val entry = Entry(handle, blob, transactionId)
      _blobCache += handle -> entry

    case _ =>
  }
}