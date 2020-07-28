package cn.pandadb.oplib.text

import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.oplib.service.ServiceInitializer

class ChineseTokenizer extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("words" -> classOf[Array[String]])

  override def extract(text: Any): Map[String, Array[String]] = {
    val words = service.segmentText(text.asInstanceOf[String]).toArray
    Map("words" -> words)
  }

}