package cn.pandadb.semoplib.image

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.{DomainType, SemanticExtractor, SubPropertyExtractor}
import org.neo4j.blob.Blob

@SemanticExtractor(name = "plateNumber", domain = DomainType.BlobImage)
class PlateNumberExtractor extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("plateNumber" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val plateNumber = service.extractPlateNumber(is)
    Map("plateNumber" -> plateNumber)
  })

}