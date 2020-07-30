package cn.pandadb.semoplib.image

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.SubPropertyExtractor
import org.neo4j.blob.Blob

class DogOrCatClassifier extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("animal" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val animal = service.classifyAnimal(is)
    Map("animal" -> animal)
  })

}
