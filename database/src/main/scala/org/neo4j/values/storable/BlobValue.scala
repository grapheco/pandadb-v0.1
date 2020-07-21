package org.neo4j.values.storable

import java.io.IOException

import org.neo4j.values.{AnyValue, ValueMapper}
import cn.pandadb.commons.blob.Blob

/**
 * common interface for BlobValue & BoltBlobValue
 */
trait BlobHolder {
  val blob: Blob;
}

trait BlobValueWriter{
  @throws(classOf[IOException])
  def writeBlob(blob: Blob)
}

/**
 * Created by bluejoe on 2018/12/12.
 */
case class BlobValue(val blob: Blob) extends ScalarValue with BlobHolder {
  override def unsafeCompareTo(value: Value): Int = blob.length.compareTo(value.asInstanceOf[BlobValue].blob.length)

  override def writeTo[E <: Exception](valueWriter: ValueWriter[E]): Unit = {
    //BlobPropertyStoreService.of(valueWriter).writeBlobValue(this, valueWriter);
    valueWriter.asInstanceOf[BlobValueWriter].writeBlob(blob);
  }

  override def asObjectCopy(): AnyRef = blob;

  override def valueGroup(): ValueGroup = ValueGroup.NO_VALUE;

  override def numberType(): NumberType = NumberType.NO_NUMBER;

  override def prettyPrint(): String = {
    val length = blob.length;
    val mimeType = blob.mimeType.text;
    s"(blob,length=$length, mimeType=$mimeType)"
  }

  override def equals(value: Value): Boolean =
    value.isInstanceOf[BlobValue] &&
      this.blob.length.equals(value.asInstanceOf[BlobValue].blob.length) &&
      this.blob.mimeType.code.equals(value.asInstanceOf[BlobValue].blob.mimeType.code);

  override def computeHash(): Int = blob.hashCode

  //TODO: map()
  override def map[T](valueMapper: ValueMapper[T]): T = this.blob.asInstanceOf[T];
}

class BlobArray(val blobs: Array[Blob]) extends AbstractBlobArray(blobs) {
  val values = blobs.map(new BlobValue(_))

  override def writeTo[E <: Exception](writer: ValueWriter[E]) {
    writer.beginArray(values.length, ValueWriter.ArrayType.BLOB)
    blobs.foreach(writer.asInstanceOf[BlobValueWriter].writeBlob(_));
    writer.endArray()
  }

  override def value(offset: Int): AnyValue = values(offset)

  override def unsafeCompareTo(other: Value): Int = if (equals(other)) 0 else -1;

  override def valueGroup(): ValueGroup = ValueGroup.NO_VALUE;

  override def map[T](mapper: ValueMapper[T]): T = this.blobs.asInstanceOf[T];

  override def equals(other: Value): Boolean = {
    other.isInstanceOf[BlobArray] &&
      other.asInstanceOf[BlobArray].blobs.zip(blobs).map(t => t._1 == t._2).reduce(_ && _)
  }
}