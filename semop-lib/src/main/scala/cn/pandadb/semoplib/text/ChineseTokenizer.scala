package cn.pandadb.semoplib.text

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.{DomainType, SemanticExtractor, SubPropertyExtractor}

@SemanticExtractor(name = "chs-tokenizer", domain = DomainType.String)
class ChineseTokenizer extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("words" -> classOf[Array[String]])

  override def extract(text: Any): Map[String, Array[String]] = {
    val words = service.segmentText(text.asInstanceOf[String]).toArray
    Map("words" -> words)
  }

}