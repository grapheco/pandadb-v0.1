package cn.pandadb.oplib.image

import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.oplib.service.ServiceInitializer
import org.neo4j.blob.Blob

class DogOrCatClassifier extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("animal" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val animal = service.classifyAnimal(is)
    Map("animal" -> animal)
  })

}
