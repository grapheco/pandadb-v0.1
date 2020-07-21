package cn.pandadb.database.blob

import java.io._
import java.net.{HttpURLConnection, URL}

import cn.pandadb.commons.blob._
import cn.pandadb.commons.util.Logging
import org.neo4j.driver.internal.types.{TypeConstructor, TypeRepresentation}
import org.neo4j.driver.internal.value.ValueAdapter
import org.neo4j.driver.v1.types.Type
import org.neo4j.values.storable.BlobHolder

class InlineBlob(bytes: Array[Byte], val length: Long, val mimeType: MimeType)
  extends Blob with Logging {

  override val streamSource: InputStreamSource = new InputStreamSource() {
    override def offerStream[T](consume: (InputStream) => T): T = {
      val fis = new ByteArrayInputStream(bytes);
      if (logger.isDebugEnabled)
        logger.debug(s"InlineBlob: length=${bytes.length}");
      val t = consume(fis);
      fis.close();
      t;
    }
  };
}

class RemoteBlob(urlConnector: String, blobId: BlobId, val length: Long, val mimeType: MimeType)
  extends Blob with Logging {

  override val streamSource: InputStreamSource = new InputStreamSource() {
    def offerStream[T](consume: (InputStream) => T): T = {
      val url = new URL(s"$urlConnector?bid=${blobId.asLiteralString()}");
      if (logger.isDebugEnabled)
        logger.debug(s"RemoteBlobValue: url=$url");
      val connection = url.openConnection().asInstanceOf[HttpURLConnection];
      connection.setDoOutput(false);
      connection.setDoInput(true);
      connection.setRequestMethod("GET");
      connection.setUseCaches(true);
      connection.setInstanceFollowRedirects(true);
      connection.setConnectTimeout(3000);
      connection.connect();
      val is = connection.getInputStream;
      val t = consume(is);
      is.close();
      t;
    }
  }
}

class BoltBlobValue(val blob: Blob)
  extends ValueAdapter with BlobHolder {

  val BOLT_BLOB_TYPE = new TypeRepresentation(TypeConstructor.BLOB);

  override def `type`(): Type = BOLT_BLOB_TYPE;

  override def equals(obj: Any): Boolean = obj.isInstanceOf[BoltBlobValue] &&
    obj.asInstanceOf[BoltBlobValue].blob.equals(this.blob);

  override def hashCode: Int = blob.hashCode()

  override def asBlob: Blob = blob;

  override def asObject = blob;

  override def toString: String = s"BoltBlobValue(blob=${blob.toString})"
}