package cn.panda.ailib.image

import cn.pandadb.commons.blob.{Blob, PropertyExtractor}
import cn.panda.ailib.service.ServiceInitializer



class DogOrCatClassifier extends PropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("animal" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val animal = service.classifyAnimal(is)
    Map("animal" -> animal)
  })

}
