package cn.pandadb.commons.blob

import java.io._
import java.net.URL
import java.util.UUID

import cn.pandadb.commons.util.StreamUtils
import cn.pandadb.commons.util.StreamUtils._
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

trait InputStreamSource {
  /**
    * note close input stream after consuming
    */
  def offerStream[T](consume: (InputStream) => T): T;
}

trait Blob extends Comparable[Blob] {
  val streamSource: InputStreamSource;
  val length: Long;
  val mimeType: MimeType;

  def offerStream[T](consume: (InputStream) => T): T = streamSource.offerStream(consume);

  def toBytes() = offerStream(IOUtils.toByteArray(_));

  override def toString = s"blob(length=${length},mime-type=${mimeType.text})";

  def makeTempFile(): File = {
    offerStream((is) => {
      val f = File.createTempFile("blob-", ".bin");
      IOUtils.copy(is, new FileOutputStream(f));
      f;
    })
  }

  override def compareTo(o: Blob) = this.length.compareTo(o.length);
}

trait StoredBlob extends Blob {
  val storeId: BlobId;
}

trait BlobId {
  def asByteArray(): Array[Byte];

  def asLiteralString(): String;
}

trait BlobIdFactory {
  def create(): BlobId;

  def readFromStream(is: InputStream): BlobId;

  def fromBytes(bytes: Array[Byte]): BlobId;

  def fromLiteralString(bid: String): BlobId;
}

class UUIDBlobIdFactory extends BlobIdFactory {
  private def fromUUID(uuid: UUID): BlobId = new BlobId() {
    def asLongArray(): Array[Long] = {
      Array[Long](uuid.getMostSignificantBits, uuid.getLeastSignificantBits);
    }

    def asByteArray(): Array[Byte] = {
      StreamUtils.convertLongArray2ByteArray(asLongArray());
    }

    override def asLiteralString(): String = {
      uuid.toString;
    }
  }

  def fromLongArray(mostSigBits: Long, leastSigBits: Long) = fromUUID(new UUID(mostSigBits, leastSigBits));

  override def create(): BlobId = fromUUID(UUID.randomUUID());

  override def fromBytes(bytes: Array[Byte]): BlobId = {
    val is = new ByteArrayInputStream(bytes);
    fromLongArray(is.readLong(), is.readLong());
  }

  override def readFromStream(is: InputStream): BlobId = {
    fromBytes(is.readBytes(16))
  }

  override def fromLiteralString(bid: String): BlobId = {
    fromUUID(UUID.fromString(bid));
  }
}

object BlobIdFactory {
  val get = new UUIDBlobIdFactory;
}

object Blob {

  class BlobImpl(val streamSource: InputStreamSource, val length: Long, val mimeType: MimeType) extends Blob {
  }

  def withStoreId(blob: Blob, bid: BlobId): StoredBlob = {
    new StoredBlob() {
      override val storeId: BlobId = bid
      override val length: Long = blob.length
      override val streamSource: InputStreamSource = blob.streamSource
      override val mimeType: MimeType = blob.mimeType
    }
  }

  def fromBytes(bytes: Array[Byte]): Blob = {
    fromInputStreamSource(new InputStreamSource() {
      override def offerStream[T](consume: (InputStream) => T): T = {
        val fis = new ByteArrayInputStream(bytes);
        val t = consume(fis);
        fis.close();
        t;
      }
    }, bytes.length, Some(MimeType.fromText("application/octet-stream")));
  }

  val EMPTY: Blob = fromBytes(Array[Byte]());

  def fromInputStreamSource(iss: InputStreamSource, length: Long, mimeType: Option[MimeType] = None) = {
    new BlobImpl(iss,
      length,
      mimeType.getOrElse(MimeType.guessMimeType(iss)));
  }

  def fromFile(file: File, mimeType: Option[MimeType] = None): BlobImpl = {
    fromInputStreamSource(new InputStreamSource() {
      override def offerStream[T](consume: (InputStream) => T): T = {
        val fis = new FileInputStream(file);
        val t = consume(fis);
        fis.close();
        t;
      }
    },
      file.length(),
      mimeType);
  }

  def fromHttpURL(url: String): Blob = {
    val client = HttpClientBuilder.create().build();
    val get = new HttpGet(url);
    val resp = client.execute(get);
    val en = resp.getEntity;
    val blob = Blob.fromInputStreamSource(new InputStreamSource() {
      override def offerStream[T](consume: (InputStream) => T): T = {
        val t = consume(en.getContent)
        client.close()
        t
      }
    }, en.getContentLength, Some(MimeType.fromText(en.getContentType.getValue)));

    blob
  }

  def fromURL(url: String): Blob = {
    val p = "(?i)(http|https|file|ftp|ftps):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?".r
    val uri = p.findFirstIn(url).getOrElse(url)

    val lower = uri.toLowerCase();
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
      fromHttpURL(uri);
    }
    else if (lower.startsWith("file://")) {
      fromFile(new File(uri.substring(lower.indexOf("//") + 1)));
    }
    else {
      //ftp, ftps?
      fromBytes(IOUtils.toByteArray(new URL(uri)));
    }
  }
}