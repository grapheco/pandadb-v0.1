package cn.pandadb.semoplib.common

import cn.pandadb.semop.{DomainType, SemanticExtractor, SubPropertyExtractor}
import org.neo4j.blob.Blob
import org.neo4j.blob.util.Configuration

/**
 * Created by bluejoe on 2019/2/17.
 */
@SemanticExtractor(name = "any", domain = DomainType.Any)
class AnyPropertyExtractor extends SubPropertyExtractor {
  override def declareProperties() = Map("class" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = {
    Map("class" -> x.getClass.getName)
  }

  override def initialize(conf: Configuration) {
  }
}

@SemanticExtractor(name = "blob", domain = DomainType.BlobAny)
class AnyBlobPropertyExtractor extends SubPropertyExtractor {
  override def declareProperties() = Map("length" -> classOf[Int], "mime" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = {
    x match {
      case b: Blob => Map("length" -> b.length, "mime" -> b.mimeType.text)
    }
  }

  override def initialize(conf: Configuration) {
  }
}