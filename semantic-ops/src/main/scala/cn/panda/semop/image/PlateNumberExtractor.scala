package cn.panda.semop.image

import cn.pandadb.commons.blob.{Blob, PropertyExtractor}
import cn.panda.semop.service.ServiceInitializer


class PlateNumberExtractor extends PropertyExtractor with ServiceInitializer {

  override def declareProperties() = Map("plateNumber" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream(is => {
    val plateNumber = service.extractPlateNumber(is)
    Map("plateNumber" -> plateNumber)
  })

}