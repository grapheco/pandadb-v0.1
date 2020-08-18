package cn.pandadb.semoplib.image

import cn.pandadb.semop.{DomainType, SemanticComparator, SingleValueComparator}
import cn.pandadb.semoplib.service.ServiceInitializer
import org.neo4j.blob.Blob

@SemanticComparator(name = "plateNumber", domains = Array(DomainType.BlobImage, DomainType.String), threshold = 0.8)
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
