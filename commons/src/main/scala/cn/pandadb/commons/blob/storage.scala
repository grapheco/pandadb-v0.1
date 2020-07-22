package cn.pandadb.commons.blob

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import cn.pandadb.commons.util.ConfigurationUtils._
import cn.pandadb.commons.util.StreamUtils._
import cn.pandadb.commons.util.{Configuration, Logging}
import org.apache.commons.io.IOUtils

trait RollbackCommand {
  def perform();
}

object BlobStorage {
  def create(conf: Configuration): BlobStorage =
    create(conf.getRaw("blob.storage"));

  def create(blobStorageClassName: Option[String]): BlobStorage = {
    blobStorageClassName.map(Class.forName(_).newInstance().asInstanceOf[BlobStorage])
      .getOrElse(createDefault())
  }

  def createDefault(): BlobStorage = new BlobStorage with Logging {
    var _blobDir: File = _;
    var _blobIdFactory: BlobIdFactory = _;

    def saveBatch(blobs: Iterable[(BlobId, Blob)]) = {
      val files = blobs.map(x => {
        val (bid, blob) = x;
        val file = fileOfBlob(bid);
        file.getParentFile.mkdirs();

        val fos = new FileOutputStream(file);
        fos.write(bid.asByteArray());
        fos.writeLong(blob.mimeType.code);
        fos.writeLong(blob.length);

        blob.offerStream { bis =>
          IOUtils.copy(bis, fos);
        }
        fos.close();

        file;
      }
      )

      new RollbackCommand() {
        override def perform(): Unit = {
          files.foreach(_.delete());
        }
      }
    }

    def loadBatch(ids: Iterable[BlobId]): Iterable[Option[Blob]] = {
      ids.map(id => Some(readFromBlobFile(fileOfBlob(id))._2));
    }

    def deleteBatch(ids: Iterable[BlobId]) = {
      ids.foreach(id => fileOfBlob(id).delete());

      new RollbackCommand() {
        override def perform(): Unit = {
          //TODO: create files?
        }
      }
    }

    private def fileOfBlob(bid: BlobId): File = {
      val idname = bid.asLiteralString();
      new File(_blobDir, s"${idname.substring(32, 36)}/$idname");
    }

    private def readFromBlobFile(blobFile: File): (BlobId, Blob) = {
      val fis = new FileInputStream(blobFile);
      val blobId = _blobIdFactory.readFromStream(fis);
      val mimeType = MimeType.fromCode(fis.readLong());
      val length = fis.readLong();
      fis.close();

      val blob = Blob.fromInputStreamSource(new InputStreamSource() {
        def offerStream[T](consume: (InputStream) => T): T = {
          val is = new FileInputStream(blobFile);
          //NOTE: skip
          is.skip(8 * 4);
          val t = consume(is);
          is.close();
          t;
        }
      }, length, Some(mimeType));

      (blobId, blob);
    }

    override def initialize(storeDir: File, blobIdFactory: BlobIdFactory, conf: Configuration): Unit = {
      _blobIdFactory = blobIdFactory;
      val baseDir: File = storeDir; //new File(conf.getRaw("unsupported.dbms.directories.neo4j_home").get());
      _blobDir = conf.getAsFile("blob.storage.file.dir", baseDir, new File(baseDir, "/blob"));
      _blobDir.mkdirs();
      logger.info(s"using storage dir: ${_blobDir.getCanonicalPath}");
    }

    override def disconnect(): Unit = {
    }
  }
}

trait BlobStorage extends Closable {
  def saveBatch(blobs: Iterable[(BlobId, Blob)]): RollbackCommand;

  def loadBatch(ids: Iterable[BlobId]): Iterable[Option[Blob]];

  def deleteBatch(ids: Iterable[BlobId]): RollbackCommand;
}

trait Closable {
  def initialize(storeDir: File, blobIdFactory: BlobIdFactory, conf: Configuration): Unit;

  def disconnect(): Unit;
}