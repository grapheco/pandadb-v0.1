package cn.pandadb.semoplib.audio

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.{DomainType, SemanticExtractor, SubPropertyExtractor}
import org.neo4j.blob.Blob

@SemanticExtractor(name = "audio", domain = DomainType.BlobAudio)
class AudioRecognizer extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("content" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val content = service.mandarinASR(is)
    Map("content" -> content)
  })
}