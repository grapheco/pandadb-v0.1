package cn.pandadb.semoplib.image

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.{DomainType, SemanticComparator, ValueSetComparator}
import org.neo4j.blob.{Blob, BlobId, URLInputStreamSource, ManagedBlob}

import scala.collection.mutable
import scala.math

@SemanticComparator(name = "face", domains = Array(DomainType.BlobImage, DomainType.BlobImage), threshold = 0.6)
class FaceSimilarityComparator extends ValueSetComparator with ServiceInitializer {

  // add a switch, cache mode or no-cache mode.
  // cache features in a map, for fast development only, optimize in the future.
  private var _featuresMap: mutable.HashMap[String, List[List[Double]]] = new mutable.HashMap[String, List[List[Double]]]()

  def compareAsSets(blob1: Any, blob2: Any): Array[Array[Double]] = {

    val featureList1 = _getFeaturesOfBlob(blob1.asInstanceOf[Blob])
    val featureList2 = _getFeaturesOfBlob(blob2.asInstanceOf[Blob])

    val arrSim: Array[Array[Double]] = featureList1.map(f1 =>
      featureList2.map(f2 =>
      _featureSimilarity(f1, f2)
    ).toArray).toArray
    arrSim
  }


  private def _isFeatureCached(blob: Blob): Boolean = {

    blob match {
      case blob: ManagedBlob => _featuresMap.contains(blob.asInstanceOf[ManagedBlob].id.asLiteralString())
      case blob: Blob => _featuresMap.contains(Blob.getMd5HexDigest(blob))
    }
  }

  private def _getFeaturesOfBlob(blob: Blob): List[List[Double]] = {
    if (_isFeatureCached(blob)) {
      _getFeatureFromCache(blob)
    } else {
      blob.offerStream(is => {
        val features = service.extractFaceFeatures(is)

        blob match {
          case blob:  ManagedBlob => {
            _featuresMap.put(blob.asInstanceOf[ManagedBlob].id.asLiteralString(), features)
          }
          case blob: Blob => {
            val key = Blob.getMd5HexDigest(blob)
            _featuresMap.put(key, features)
          }
        }
        features
      })
    }
  }

  private def _getFeatureFromCache(blob: Blob): List[List[Double]] = {
    blob match {
      case blob: ManagedBlob => _featuresMap.get(blob.id.asLiteralString()).get
      case blob: Blob => _featuresMap.get(Blob.getMd5HexDigest(blob)).get
    }
  }

  // not cosine distance
  def _featureSimilarity(feature1: List[Double], feature2: List[Double]): Double = {
    if(feature1.length != feature2.length) {
      throw new Exception("Face feature's length not equal.")
    }
    val delta: List[Double] = feature1.zip(feature2).map(pair => pair._1 - pair._2)
    val norm = math.sqrt(delta.map(math.pow(_, 2)).sum)
    val sim = 1 - norm
    sim
  }

}