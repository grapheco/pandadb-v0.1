package cn.pandadb.semoplib.image

import cn.pandadb.semop.SingleValueComparator
import cn.pandadb.semoplib.service.ServiceInitializer
import org.neo4j.blob.Blob

class PlateNumber2TextSimilarityComparator extends SingleValueComparator with ServiceInitializer {
  override def compare(a: Any, b: Any): Double = {
    (a, b) match {
      case (blob: Blob, text: String) =>
        blob.offerStream { is =>
          if (service.extractPlateNumber(is).matches(text))
            1.0
          else
            0.0
        }
    }
  }
}
