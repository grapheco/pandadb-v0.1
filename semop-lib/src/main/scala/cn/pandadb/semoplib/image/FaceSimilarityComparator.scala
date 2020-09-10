package cn.pandadb.semoplib.image

import cn.pandadb.semoplib.service.ServiceInitializer
import cn.pandadb.semop.{DomainType, SemanticComparator, ValueSetComparator}
import org.neo4j.blob.{Blob, BlobId, ManagedBlob}

import scala.collection.mutable
import scala.math

@SemanticComparator(name = "face", domains = Array(DomainType.BlobImage, DomainType.BlobImage), threshold = 0.6)
class FaceSimilarityComparator extends ValueSetComparator with ServiceInitializer {

  // cache features in a map, for fast development only, optimize in the future.
  // TODO: ask bluejoe how to set the key
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

    if(blob.isInstanceOf[ManagedBlob]){
      val flag = _featuresMap.contains(blob.asInstanceOf[ManagedBlob].id.asLiteralString())
      flag
    } else {
      false
    }
  }

  private def _getFeaturesOfBlob(blob: Blob): List[List[Double]] = {
    if (_isFeatureCached(blob)) {
      _getFeatureFromCache(blob.asInstanceOf[ManagedBlob])
    } else {
      blob.offerStream(is => {
        val features = service.extractFaceFeatures(is)
//        val nowLiteralValue = blob.asInstanceOf[ManagedBlob].id.asLiteralString()
        // cache the feature if it's ManagedBlob
        if(blob.isInstanceOf[ManagedBlob]) {
          _featuresMap.put(blob.asInstanceOf[ManagedBlob].id.asLiteralString(), features)
        }
        features
      })
    }
  }

  private def _getFeatureFromCache(blob: ManagedBlob): List[List[Double]] = {
    _featuresMap.get(blob.id.asLiteralString()).get
  }

  def _featureSimilarity(featureList1: List[Double], featureList2: List[Double]): Double = {

    if(featureList1.length != featureList2.length) {
      throw new Exception("Face feature's length not equal.")
    }

    val lenFeature1 = math.sqrt(featureList1.map(math.pow(_,2)).sum)
    val lenFeature2 = math.sqrt(featureList2.map(math.pow(_,2)).sum)
    if(lenFeature1==0 || lenFeature2==0){
      0
    } else {
      val innerProduct = featureList1.zip(featureList2).map(pair => pair._1 * pair._2).sum

      val cosine = innerProduct/lenFeature1/lenFeature2

      cosine
    }

  }


}