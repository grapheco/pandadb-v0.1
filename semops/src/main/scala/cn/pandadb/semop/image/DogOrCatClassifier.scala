package cn.pandadb.semop.image

import cn.pandadb.commons.blob.Blob
import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.semop.service.ServiceInitializer



class DogOrCatClassifier extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("animal" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val animal = service.classifyAnimal(is)
    Map("animal" -> animal)
  })

}
