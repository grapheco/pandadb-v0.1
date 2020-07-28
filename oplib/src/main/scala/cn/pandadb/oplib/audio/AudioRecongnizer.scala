package cn.pandadb.oplib.audio

import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.oplib.service.ServiceInitializer
import org.neo4j.blob.Blob

class AudioRecongnizer extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("content" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val content = service.mandarinASR(is)
    Map("content" -> content)
  })

}
