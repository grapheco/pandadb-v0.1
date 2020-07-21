package cn.pandadb.blobstorage.buffer

import java.io.File
import java.util.concurrent._

import cn.pandadb.commons.blob.BlobIdFactory
import cn.pandadb.commons.blob.storage.Closable
import cn.pandadb.blobstorage.storage.{Bufferable, ExternalBlobStorage}
import cn.pandadb.blobstorage.buffer.exception.NotBindingException
import cn.pandadb.commons.util.Configuration
import cn.pandadb.commons.util.ConfigurationUtils._

trait Buffer extends Closable {
  protected final var temp : ExternalBlobStorage with Bufferable = _ // only delete
  protected final var persist : ExternalBlobStorage = _
  protected final var binding : Boolean = false
  protected final val es : ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  def getBufferableStorage: ExternalBlobStorage with Bufferable = temp
  def bind(tempStorage : ExternalBlobStorage with Bufferable, externalStorage : ExternalBlobStorage) : Unit = {
    this.temp = tempStorage
    this.persist = externalStorage
    binding = true
  }

  def checkInit() : Unit = {
    if (!binding) throw new NotBindingException("Haven't binding the storage")
  }

}

/**
  * a simple implement of buffer
  * one thread to carry the blob from the bufferable storage to another storage
  * every time the thread run will upload *all* the current blob store in bufferable storage to external storage
  *
  * @param tempStorage a bufferable storage
  * @param externalStorage a storage like file or hbase
  * @param time the interval upload time gap
  */
class RollBackBuffer(tempStorage : ExternalBlobStorage with Bufferable, externalStorage : ExternalBlobStorage
                     , time : Long) extends Buffer with RollBack {
  override def initialize(storeDir: File, blobIdFac : BlobIdFactory, conf: Configuration): Unit = {
    bind(tempStorage, externalStorage)
    val failureLog = conf.getValueAsString("blob.storage.rollback.logpath","/log/rb.log")
    val logFile = new File(failureLog)
    if (logFile.exists()) RollBack(persist, new RollBackLogReader(logFile))
    es.schedule(new Runnable {
      val failureWriter = new RollBackLogWriter(logFile)
      override def run(): Unit = {
        while (true) {
          val blobIds = temp.getAllBlob
          failureWriter.append(blobIds.map(_.asLiteralString()))
          persist.saveBatch(blobIds zip tempStorage.loadBlobBatch(blobIds))
          failureWriter.clean()
        }
      }
    }, time, TimeUnit.MILLISECONDS)
  }
  override def disconnect(): Unit = {
    if(binding) {
      persist.disconnect()
    }
    if (es != null) es.awaitTermination(3, TimeUnit.SECONDS)
  }
}

