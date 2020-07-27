package cn.pandadb.database

import java.io.File

import cn.pandadb.commons.blob.Blob
import cn.pandadb.query.BlobFactory
import org.neo4j.cypher.internal.frontend.v3_4.parser.{BlobBase64URL, BlobFileURL, BlobFtpURL, BlobHttpURL, BlobURL}

class SimpleBlobFactory extends BlobFactory {
  override def createBlob(url: BlobURL): Blob = {
    url match{
      case BlobHttpURL(url) =>
        Blob.fromHttpURL(url)
      case BlobFtpURL(url)=>
        Blob.fromURL(url)
      case BlobFileURL(filePath)=>
        Blob.fromFile(new File(filePath))
    }

  }
}
