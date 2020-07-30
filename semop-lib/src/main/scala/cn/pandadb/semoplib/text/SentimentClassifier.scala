package cn.pandadb.semoplib.text

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.SubPropertyExtractor

class SentimentClassifier extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("sentiment" -> classOf[String])

  override def extract(text: Any): Map[String, Any] = {

    val sentiment = service.sentimentClassifier(text.asInstanceOf[String])
    Map("sentiment" -> sentiment)
  }

}

