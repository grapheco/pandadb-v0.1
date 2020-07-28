package cn.pandadb.oplib.image

import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.oplib.service.ServiceInitializer
import org.neo4j.blob.Blob

class PlateNumberExtractor extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("plateNumber" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val plateNumber = service.extractPlateNumber(is)
    Map("plateNumber" -> plateNumber)
  })

}