package cn.panda.semop.text

import cn.pandadb.commons.blob.PropertyExtractor
import cn.panda.semop.service.ServiceInitializer
import cn.pandadb.commons.util.Configuration
import cn.pandadb.commons.util.ConfigurationUtils._

class SentimentClassifier extends PropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("sentiment" -> classOf[String])

  override def extract(text: Any): Map[String, Any] = {

    val sentiment = service.sentimentClassifier(text.asInstanceOf[String])
    Map("sentiment" -> sentiment)
  }

}

