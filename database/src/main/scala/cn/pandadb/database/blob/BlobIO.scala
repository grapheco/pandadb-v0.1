package cn.pandadb.database.blob

import java.io.{ByteArrayOutputStream, File}

import cn.pandadb.commons.blob._
import cn.pandadb.commons.blob.BlobStorage
import cn.pandadb.commons.util.ReflectUtils._
import cn.pandadb.commons.util.StreamUtils._
import cn.pandadb.commons.util.{Configuration, Logging, StreamUtils}
import cn.pandadb.commons.RuntimeContext
import cn.pandadb.database.{BlobCacheInSession, BlobPropertyStoreService, BoundTransactionState, ThreadBoundContext}
import org.neo4j.driver.v1.Value
import org.neo4j.kernel.api.{KernelTransaction, TransactionHook}
import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.api.TransactionHooks
import org.neo4j.kernel.impl.store.PropertyType
import org.neo4j.kernel.impl.store.record.{PrimitiveRecord, PropertyBlock, PropertyRecord}
import org.neo4j.kernel.impl.transaction.state.RecordAccess.RecordProxy
import org.neo4j.kernel.impl.transaction.state.{PropertyDeleter, RecordAccess}
import org.neo4j.kernel.internal.TransactionEventHandlers.TransactionHandlerState
import org.neo4j.storageengine.api.txstate.ReadableTransactionState
import org.neo4j.storageengine.api.{StorageStatement, StoreReadLayer}
import org.neo4j.values.AnyValue
import org.neo4j.values.storable._

/**
  * Created by bluejoe on 2018/7/4.
  */
object BlobIO extends Logging {
  val BOLT_VALUE_TYPE_BLOB_INLINE = org.neo4j.driver.internal.packstream.PackStream.RESERVED_C5;
  val BOLT_VALUE_TYPE_BLOB_REMOTE = org.neo4j.driver.internal.packstream.PackStream.RESERVED_C4;
  val MAX_INLINE_BLOB_BYTES = 10240;

  val blobIdFactory = BlobIdFactory.get
  //10k

  def decodeBlob(bytes: Array[Byte]): Blob = {
    _readBlobValue(StreamUtils.convertByteArray2LongArray(bytes)).blob;
  }

  def addBlobFlushHook(tx: KernelTransaction): Unit = {
    val hooks = tx._get("hooks").asInstanceOf[TransactionHooks];
    hooks.register(new TransactionHook[TransactionHandlerState]() {
      override def afterRollback(state: ReadableTransactionState, transaction: KernelTransaction, outcome: TransactionHandlerState): Unit = {
      }

      override def afterCommit(state: ReadableTransactionState, transaction: KernelTransaction, outcome: TransactionHandlerState): Unit = {
        ThreadBoundContext.blobBuffer.flushBlobs();
        ThreadBoundContext.conf.contextGetOption[BlobCacheInSession].map(_.invalidate(ThreadBoundContext.streamingBlobs.ids()));
      }

      override def beforeCommit(state: ReadableTransactionState, transaction: KernelTransaction, storeReadLayer: StoreReadLayer, statement: StorageStatement): TransactionHandlerState = {
        null;
      }
    })
  }

  def encodeBlob(blob: Blob): Array[Byte] = {
    val baos = new ByteArrayOutputStream();
    val blobId = blobIdFactory.create();
    _encodeBlobEntryAsLongArray(blob, blobId, 0).foreach(baos.writeLong(_));

    ThreadBoundContext.blobBuffer.addBlob(blobId, blob);
    baos.toByteArray;
  }

  def writeBlobArray(blob: Array[Blob], packer: org.neo4j.driver.internal.packstream.PackStream.Packer): Unit = {
    //TOOD
    throw new UnsupportedOperationException();
  }

  //client side?
  def writeBlobOnBoltClientSide(blob: Blob, packer: org.neo4j.driver.internal.packstream.PackStream.Packer): Unit = {
    val out = packer._get("out").asInstanceOf[org.neo4j.driver.internal.packstream.PackOutput];
    val out2 =
      new PackOutputInterface() {
        override def writeByte(b: Byte): Unit = out.writeByte(b);

        override def writeInt(i: Int): Unit = out.writeInt(i);

        override def writeBytes(bs: Array[Byte]): Unit = out.writeBytes(bs);

        override def writeLong(l: Long): Unit = out.writeLong(l);
      }

    _outputBlob(blob, out2, useInlineAlways = true);
  }

  private def _outputBlob(blob: Blob, out: PackOutputInterface, useInlineAlways: Boolean): Unit = {
    //create a temp blodid
    val tempBlobId = blobIdFactory.create();
    val inline = useInlineAlways || (blob.length <= MAX_INLINE_BLOB_BYTES);
    //write marker
    out.writeByte(if (inline) {
      BOLT_VALUE_TYPE_BLOB_INLINE
    }
    else {
      BOLT_VALUE_TYPE_BLOB_REMOTE
    });

    //write blob entry
    _encodeBlobEntryAsLongArray(blob, tempBlobId).foreach(out.writeLong(_));

    //write inline
    if (inline) {
      val bytes = blob.toBytes();
      out.writeBytes(bytes);
    }
    else {
      //write as a HTTP resource
      val config = ThreadBoundContext.conf;
      val httpConnectorUrl: String = config.asInstanceOf[RuntimeContext].contextGet("blob.server.connector.url").asInstanceOf[String];
      val bpss = config.asInstanceOf[RuntimeContext].contextGet[BlobPropertyStoreService];
      //http://localhost:1224/blob
      val bs = httpConnectorUrl.getBytes("utf-8");
      out.writeInt(bs.length);
      out.writeBytes(bs);
      config.asInstanceOf[RuntimeContext].contextGet[BlobCacheInSession].put(tempBlobId, blob);
    }
  }

  private def _encodeBlobEntryAsLongArray(blob: Blob, blobId: BlobId, keyId: Int = 0): Array[Long] = {
    val values = new Array[Long](4);
    //val digest = ByteArrayUtils.convertByteArray2LongArray(blob.digest);
    /*
    blob uses 4*8 bytes: [v0][v1][v2][v3]
    v0: [____,____][____,____][____,____][____,____][[____,tttt][kkkk,kkkk][kkkk,kkkk][kkkk,kkkk] (t=type, k=keyId)
    v1: [llll,llll][llll,llll][llll,llll][llll,llll][llll,llll][llll,llll][mmmm,mmmm][mmmm,mmmm] (l=length, m=mimeType)
    v2: [iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii]
    v3: [iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii][iiii,iiii]
    */
    values(0) = keyId | (PropertyType.BLOB.intValue() << 24);
    values(1) = blob.mimeType.code | (blob.length << 16);

    val la = StreamUtils.convertByteArray2LongArray(blobId.asByteArray());
    values(2) = la(0);
    values(3) = la(1);

    values;
  }

  def writeBlobOnBoltServerSide(blob: Blob, packer: org.neo4j.bolt.v1.packstream.PackStream.Packer): Unit = {
    val out = packer._get("out").asInstanceOf[org.neo4j.bolt.v1.packstream.PackOutput];
    val out2 =
      new PackOutputInterface() {
        override def writeByte(b: Byte): Unit = out.writeByte(b);

        override def writeInt(i: Int): Unit = out.writeInt(i);

        override def writeBytes(bs: Array[Byte]): Unit = out.writeBytes(bs, 0, bs.length);

        override def writeLong(l: Long): Unit = out.writeLong(l);
      }

    _outputBlob(blob, out2, useInlineAlways = false);
  }

  def readBlobFromBoltStreamIfAvailable(unpacker: org.neo4j.driver.internal.packstream.PackStream.Unpacker): Value = {
    val in = unpacker._get("in").asInstanceOf[org.neo4j.driver.internal.packstream.PackInput];
    _readBlobFromBoltStreamIfAvailable(new PackInputInterface() {
      def peekByte(): Byte = in.peekByte();

      def readByte(): Byte = in.readByte();

      def readBytes(bytes: Array[Byte], offset: Int, toRead: Int) = in.readBytes(bytes, offset, toRead);

      def readInt(): Int = in.readInt();

      def readLong(): Long = in.readLong();
    }).map(new BoltBlobValue(_)).getOrElse(null);
  }

  private def _readBlobFromBoltStreamIfAvailable(in: PackInputInterface): Option[Blob] = {
    val byte = in.peekByte();
    byte match {
      case BOLT_VALUE_TYPE_BLOB_REMOTE =>
        in.readByte();

        val values = for (i <- 0 to 3) yield in.readLong();
        val (bid, length, mt) = _unpackBlob(values.toArray);

        val lengthUrl = in.readInt();
        val bs = new Array[Byte](lengthUrl);
        in.readBytes(bs, 0, lengthUrl);

        val url = new String(bs, "utf-8");
        //TODO: reuse bolt session
        Some(new RemoteBlob(url, bid, length, mt));

      //no inline
      case BOLT_VALUE_TYPE_BLOB_INLINE =>
        in.readByte();

        val values = for (i <- 0 to 3) yield in.readLong();
        val (_, length, mt) = _unpackBlob(values.toArray);

        //read inline
        val bs = new Array[Byte](length.toInt);
        in.readBytes(bs, 0, length.toInt);
        Some(new InlineBlob(bs, length, mt));

      case _ => None;
    }
  }

  def _unpackBlob(values: Array[Long]): (BlobId, Long, MimeType) = {
    //val keyId = PropertyBlock.keyIndexId(values(0));
    val length = values(1) >> 16;
    val mimeType = values(1) & 0xFFFFL;

    val bid = blobIdFactory.fromLongArray(values(2), values(3));
    val mt = MimeType.fromCode(mimeType);
    (bid, length, mt);
  }

  def readBlobFromBoltStreamIfAvailable(unpacker: org.neo4j.bolt.v1.packstream.PackStream.Unpacker): AnyValue = {
    val in = unpacker._get("in").asInstanceOf[org.neo4j.bolt.v1.packstream.PackInput];
    _readBlobFromBoltStreamIfAvailable(new PackInputInterface() {
      def peekByte(): Byte = in.peekByte();

      def readByte(): Byte = in.readByte();

      def readBytes(bytes: Array[Byte], offset: Int, toRead: Int) = in.readBytes(bytes, offset, toRead);

      def readInt(): Int = in.readInt();

      def readLong(): Long = in.readLong();
    }).map(new BlobValue(_)).getOrElse(null);
  }

  implicit def wrapNeo4jConf(conf: Config): Configuration = new Configuration() {
    override def getRaw(name: String): Option[String] = {
      val raw = conf.getRaw(name);
      if (raw.isPresent) {
        Some(raw.get())
      }
      else {
        None
      }
    }
  }

  def startBlobBatchImport(storeDir: File, arg: Config): BlobBatchImportSession = {
    val state = new BoundTransactionState() {
      override val conf: RuntimeContext = arg.asInstanceOf[RuntimeContext];
      override lazy val blobStorage: BlobStorage = BlobStorage.create(arg);
      blobStorage.initialize(storeDir, BlobIdFactory.get, arg);
    }

    ThreadBoundContext.bindState(state);

    new BlobBatchImportSession() {
      def success(): Unit = {
        state.blobBuffer.flushBlobs();
        ThreadBoundContext.unbindState();
      }

      def failure(): Unit = {
        ThreadBoundContext.unbindState();
      }
    }
  }

  def saveBlob(blob: Blob, valueWriter: ValueWriter[_]) = {
    val blobId = blobIdFactory.create();
    val keyId = valueWriter._get("keyId").asInstanceOf[Int];
    val block = valueWriter._get("block").asInstanceOf[PropertyBlock];

    ThreadBoundContext.blobBuffer.addBlob(blobId, blob);
    block.setValueBlocks(_encodeBlobEntryAsLongArray(blob, blobId, keyId));
  }

  def readBlobValue(values: Array[Long]): BlobValue = {
    _readBlobValue(values);
  }

  def readBlobValue(block: PropertyBlock): BlobValue = {
    _readBlobValue(block.getValueBlocks);
  }

  private def _readBlobValue(values: Array[Long]): BlobValue = {
    //FIXME
    //val conf = ThreadBoundContext.transaction._conf;
    val (bid, length, mt) = _unpackBlob(values);
    val storage: BlobStorage = ThreadBoundContext.conf.contextGet[BlobStorage];
    /*
    val blob = ThreadBoundContext.transaction._stateOption
      .flatMap(_.asInstanceOf[TxStateExtension].getBufferedBlob(bid))
      .getOrElse(storage.loadBatch(Array(bid)).head.get);
    */
    val blob = ThreadBoundContext.blobBuffer.getBlob(bid)
      .getOrElse(storage.loadBatch(Array(bid)).head.get);
    new BlobValue(Blob.withStoreId(blob, bid));
  }

  def deleteBlobProperty(deleter: PropertyDeleter, primitiveProxy: RecordProxy[_, Void],
                         propertyKey: Int,
                         propertyRecords: RecordAccess[PropertyRecord, PrimitiveRecord],
                         block: PropertyBlock): Unit = {
    val values = block.getValueBlocks;
    val length = values(1) >> 16;

    //val state = ThreadBoundContext.transaction._state.asInstanceOf[TxStateExtension];
    val bid = blobIdFactory.fromLongArray(values(2), values(3));
    //soft delete
    //state.removeBlob(bid);
    ThreadBoundContext.blobBuffer.removeBlob(bid);
  }

  /**
    * common interface for org.neo4j.driver.internal.packstream.PackOutput & org.neo4j.bolt.v1.packstream.PackOutput
    */
  trait PackOutputInterface {
    def writeByte(b: Byte);

    def writeLong(l: Long);

    def writeInt(i: Int);

    def writeBytes(bs: Array[Byte]);
  }

  trait PackInputInterface {
    def peekByte(): Byte;

    def readByte(): Byte;

    def readBytes(bytes: Array[Byte], offset: Int, toRead: Int);

    def readInt(): Int;

    def readLong(): Long;
  }

  trait BlobBatchImportSession {
    def success(): Unit;

    def failure(): Unit;
  }

}