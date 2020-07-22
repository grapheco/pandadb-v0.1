package cn.pandadb.semop.audio

import cn.pandadb.commons.blob.Blob
import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.semop.service.ServiceInitializer


class AudioRecongnizer extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("content" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val content = service.mandarinASR(is)
    Map("content" -> content)
  })

}
