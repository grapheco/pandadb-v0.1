package cn.panda.semop.audio

import cn.pandadb.commons.blob.{Blob, PropertyExtractor}
import cn.panda.semop.service.ServiceInitializer


class AudioRecongnizer extends PropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("content" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val content = service.mandarinASR(is)
    Map("content" -> content)
  })

}
