package cn.pandadb.database.blob

import java.io.{File, FileInputStream, InputStream}

import cn.pandadb.commons.blob.{Blob, InputStreamSource, MimeType}
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.neo4j.procedure.{Description, Name, UserFunction}

/**
  * Created by bluejoe on 2018/7/22.
  */
class DefaultBlobFunctions {
  @UserFunction("Blob.fromFile")
  @Description("generate a blob object from the given file")
  def fromFile(@Name("filePath") filePath: String): Blob = {
    if (filePath == null || filePath.trim.isEmpty) {
      throw new CypherFunctionException(s"invalid file path: $filePath");
    }

    val file = new File(filePath);
    if (!file.exists()) {
      throw new CypherFunctionException(s"file not found: $filePath");
    }

    Blob.fromFile(file);
  }

  @UserFunction("Blob.fromURL")
  @Description("load blob from an url")
  def fromURL(@Name("url") url: String): Blob = {
    Blob.fromURL(url);
  }

  @UserFunction("Blob.fromUTF8String")
  @Description("generate a blob object from the given file")
  def fromUTF8String(@Name("text") text: String): Blob = {
    Blob.fromBytes(text.getBytes("utf-8"));
  }

  @UserFunction("Blob.fromString")
  @Description("generate a blob object from the given file")
  def fromString(@Name("text") text: String, @Name("encoding") encoding: String): Blob = {
    Blob.fromBytes(text.getBytes(encoding));
  }

  @UserFunction("Blob.fromBytes")
  @Description("generate a blob object from the given file")
  def fromBytes(@Name("bytes") bytes: Array[Byte]): Blob = {
    Blob.fromBytes(bytes);
  }

  @UserFunction("Bytes.guessType")
  @Description("guess mime type of a byte array")
  def guessBytesMimeType(@Name("bytes") bytes: Array[Byte]): String = {
    MimeType.guessMimeType(Blob.fromBytes(bytes).streamSource).toString;
  }

  @UserFunction("Blob.guessType")
  @Description("guess mime type of a blob")
  def guessBlobMimeType(@Name("blob") blob: Blob): String = {
    MimeType.guessMimeType(blob.streamSource).toString;
  }

  @UserFunction("Blob.empty")
  @Description("generate an empty blob")
  def empty(): Blob = {
    Blob.EMPTY
  }

  @UserFunction("Blob.len")
  @Description("get length of a blob object")
  def getBlobLength(@Name("blob") blob: Blob): Long = {
    if (blob == null) {
      null.asInstanceOf[Long]
    }
    else {
      blob.length
    }
  }

  @UserFunction("Blob.toString")
  @Description("cast to a string")
  def cast2String(@Name("blob") blob: Blob, @Name("encoding") encoding: String): String = {
    if (blob == null) {
      null
    }
    else {
      new String(blob.toBytes(), encoding);
    }
  }

  @UserFunction("Bytes.fromFile")
  @Description("load bytes from a file")
  def bytesFromFile(@Name("filePath") filePath: String): Array[Byte] = {
    val file = new File(filePath);
    val fis = new FileInputStream(file);
    val bytes = IOUtils.toByteArray(fis);
    fis.close();
    bytes;
  }

  @UserFunction("Blob.toUTF8String")
  @Description("cast to a string in utf-8 encoding")
  def cast2UTF8String(@Name("blob") blob: Blob): String = {
    if (blob == null) {
      null
    }
    else {
      new String(blob.toBytes(), "utf-8");
    }
  }

  @UserFunction("Blob.toBytes")
  @Description("cast to a byte array")
  def cast2Bytes(@Name("blob") blob: Blob): Array[Byte] = {
    blob.toBytes();
  }

  @UserFunction("Blob.mime")
  @Description("get mime type of a blob object")
  def getMimeType(@Name("blob") blob: Blob): String = {
    if (blob == null) {
      null
    }
    else {
      blob.mimeType.text
    }
  }

  @UserFunction("Blob.mime1")
  @Description("get mime type of a blob object")
  def getMajorMimeType(@Name("blob") blob: Blob): String = {
    if (blob == null) {
      null
    }
    else {
      blob.mimeType.text.split("/")(0)
    }
  }

  @UserFunction("Blob.mime2")
  @Description("get mime type of a blob object")
  def getMinorMimeType(@Name("blob") blob: Blob): String = {
    if (blob == null) {
      null
    }
    else {
      blob.mimeType.text.split("/")(1);
    }
  }

  @UserFunction("Blob.is")
  @Description("determine if the blob is kind of specified mime type")
  def is(@Name("blob") blob: Blob, @Name("mimeType") mimeType: String): Boolean = {
    if (blob == null) {
      false
    }
    else {
      val a = mimeType.split("/");
      if (a.length == 1) {
        blob.mimeType.major.equalsIgnoreCase(a(0))
      }
      else {
        blob.mimeType.major.equalsIgnoreCase(a(0)) && blob.mimeType.minor.equalsIgnoreCase(a(1))
      }
    }
  }
}

class CypherFunctionException(msg: String) extends RuntimeException(msg) {

}