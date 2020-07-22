package cn.pandadb.semop.image

import cn.pandadb.commons.blob.Blob
import cn.pandadb.commons.semop.SubPropertyExtractor
import cn.pandadb.semop.service.ServiceInitializer


class PlateNumberExtractor extends SubPropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("plateNumber" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val plateNumber = service.extractPlateNumber(is)
    Map("plateNumber" -> plateNumber)
  })

}