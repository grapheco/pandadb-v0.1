package cn.pandadb.blobstorage.storage

import java.io._

import cn.pandadb.commons.blob._
import cn.pandadb.commons.blob.storage.{BlobStorage, RollbackCommand}
import cn.pandadb.blobstorage.buffer.Buffer
import cn.pandadb.blobstorage.util.FileUtils
import cn.pandadb.commons.util.ConfigurationUtils._
import cn.pandadb.commons.util.StreamUtils._
import cn.pandadb.commons.util.{Configuration, Logging}
import org.apache.commons.io.IOUtils
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder.ModifyableColumnFamilyDescriptor
import org.apache.hadoop.hbase.client.TableDescriptorBuilder.ModifyableTableDescriptor
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}

import scala.collection.JavaConversions._
import scala.concurrent.forkjoin.ForkJoinPool


trait Bufferable {

  def getAllBlob: Iterable[BlobId]

  def loadBlobBatch(BlobIds: Iterable[BlobId]): Iterable[Blob]

}

//FIXME: choose a better class name, this class is designed only for blob storage, not for nodes/properties
trait ExternalBlobStorage extends BlobStorage with Logging {
  protected var _blobIdFac: BlobIdFactory = _

  def getIdFac: BlobIdFactory = _blobIdFac

  def check(bid: BlobId): Boolean

  def delete(bid: BlobId): RollbackCommand;

  def save(bid: BlobId, blob: Blob): RollbackCommand;

  def load(bid: BlobId): Option[Blob];

  def createRollbackCommand(commands: Iterable[RollbackCommand]): RollbackCommand =
    new RollbackCommand() {
      override def perform(): Unit = commands.foreach(_.perform())
    }

  def deleteBatch(bids: Iterable[BlobId]): RollbackCommand = createRollbackCommand(bids.map(delete))

  def saveBatch(blobs: Iterable[(BlobId, Blob)]): RollbackCommand = createRollbackCommand(blobs.map(a => save(a._1, a._2)))

  def checkExistBatch(bids: Iterable[BlobId]): Iterable[Boolean] = bids.map(check)

  def loadBatch(bids: Iterable[BlobId]): Iterable[Option[Blob]] = bids.map(load)
}

// TODO ref
// TODO externalStorage?
class HybridBlobStorage(persistStorage: ExternalBlobStorage, val buffer: Buffer) extends ExternalBlobStorage {

  override def deleteBatch(bids: Iterable[BlobId]): RollbackCommand = {
    buffer.getBufferableStorage.deleteBatch(bids)
    persistStorage.deleteBatch(bids)
  }

  override def saveBatch(blobs: Iterable[(BlobId, Blob)]): RollbackCommand = buffer.getBufferableStorage.saveBatch(blobs)

  override def checkExistBatch(bids: Iterable[BlobId]): Iterable[Boolean] = bids.map(f =>
    buffer.getBufferableStorage.check(f) || persistStorage.check(f))

  override def loadBatch(bids: Iterable[BlobId]): Iterable[Option[Blob]] = persistStorage.loadBatch(bids)

  override def initialize(storeDir: File, blobIdFac: BlobIdFactory, conf: Configuration): Unit = {
    buffer.checkInit()
    buffer.initialize(storeDir, blobIdFac, conf)
  }

  override def disconnect(): Unit = {
    buffer.disconnect()
    persistStorage.disconnect()
  }

  override def save(bid: BlobId, blob: Blob): RollbackCommand = buffer.getBufferableStorage.save(bid, blob)

  //FIXME: what if it exist in buffer?
  override def load(bid: BlobId): Option[Blob] = persistStorage.load(bid)

  override def check(bid: BlobId): Boolean = buffer.getBufferableStorage.check(bid) || persistStorage.check(bid)

  override def delete(bid: BlobId): RollbackCommand = {
    buffer.getBufferableStorage.delete(bid)
    persistStorage.delete(bid)
  }
}

class FileBlobStorage extends ExternalBlobStorage with Bufferable {
  var _blobDir: File = _

  override def initialize(storeDir: File, blobIdFac: BlobIdFactory, conf: Configuration): Unit = {
    val baseDir: File = storeDir; //new File(conf.getRaw("unsupported.dbms.directories.neo4j_home").get());
    _blobDir = conf.getAsFile("blob.storage.file.dir", baseDir, new File(baseDir, "/blob"))
    if (!_blobDir.exists()) {
      _blobDir.mkdirs()
    }
    _blobIdFac = blobIdFac
    logger.info(s"using storage dir: ${_blobDir.getCanonicalPath}")
  }

  override def disconnect(): Unit = {
  }

  override def delete(bid: BlobId): RollbackCommand = {
    val f = FileUtils.blobFile(bid, _blobDir)
    if (f.exists()) f.delete()

    new RollbackCommand {
      override def perform(): Unit = {
        //TODO
      }
    }
  }

  override def getAllBlob: Iterable[BlobId] = FileUtils.listAllFiles(_blobDir).filter(f => f.isFile).map(f => _blobIdFac.fromLiteralString(f.getName))

  override def loadBlobBatch(bids: Iterable[BlobId]): Iterable[Blob] = {
    bids.map { bid =>
      FileUtils.readFromBlobFile(FileUtils.blobFile(bid, _blobDir), _blobIdFac)._2
    }
  }

  override def save(bid: BlobId, blob: Blob): RollbackCommand = {
    val file = FileUtils.blobFile(bid, _blobDir)
    file.getParentFile.mkdirs()

    val fos = new FileOutputStream(file)
    fos.write(bid.asByteArray())
    fos.writeLong(blob.mimeType.code)
    fos.writeLong(blob.length)

    blob.offerStream { bis =>
      IOUtils.copy(bis, fos);
    }
    fos.close()

    new RollbackCommand {
      override def perform(): Unit = {
        //TODO
      }
    }
  }

  //FIXME: consider that it does not exist
  override def load(bid: BlobId): Option[Blob] = Some(FileUtils.readFromBlobFile(FileUtils.blobFile(bid, _blobDir), _blobIdFac)._2)

  override def check(bid: BlobId): Boolean = FileUtils.blobFile(bid, _blobDir).exists()
}