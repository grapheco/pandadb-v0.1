package cn.pandadb.database

import java.io.File

import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.blob.BlobStorage

trait BlobBatchImportSession {
  def success(): Unit;

  def failure(): Unit;
}

object BlobBatchImportSession {
  def start(storeDir: File, arg: Config): BlobBatchImportSession = {
    /*
    val state = new BoundTransactionState() {
      override val conf: RuntimeContext = arg.asInstanceOf[RuntimeContext];
      override lazy val blobStorage: BlobStorage = BlobStorage.create(arg);
      blobStorage.initialize(storeDir, BlobIdFactory.get, arg);
    }
  */
    //ThreadBoundContext.bindState(state);

    new BlobBatchImportSession() {
      def success(): Unit = {
        //state.blobBuffer.flushBlobs();
        //ThreadBoundContext.unbindState();
      }

      def failure(): Unit = {
        //ThreadBoundContext.unbindState();
      }
    }
  }
}

object ThreadBoundContext {
  def bindState(state: Any) = throw new UnsupportedOperationException
}
