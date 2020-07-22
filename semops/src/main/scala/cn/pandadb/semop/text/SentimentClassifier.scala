package cn.pandadb.semop.text

import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.semop.service.ServiceInitializer

class SentimentClassifier extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("sentiment" -> classOf[String])

  override def extract(text: Any): Map[String, Any] = {

    val sentiment = service.sentimentClassifier(text.asInstanceOf[String])
    Map("sentiment" -> sentiment)
  }

}

