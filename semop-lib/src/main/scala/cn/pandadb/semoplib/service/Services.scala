package cn.pandadb.semoplib.service

import java.io.InputStream

import cn.pandadb.semop.PropertyValueComparator
import org.neo4j.blob.util.ConfigUtils._
import org.neo4j.blob.util.Configuration
import org.neo4j.values.AnyValue

import scala.collection.immutable.Map
import scala.util.parsing.json.JSON

class Services(private val _aipmHttpHostUrl: String) {
  //TODO: hard coding, try <baseUrl>/service/<algorithm name>
  val servicesPath = Map(
    "FaceSim" -> "service/face/similarity/",
    "FaceInPhoto" -> "service/face/in_photo/",
    "FaceFeature" -> "service/face/feature/",
    "PlateNumber" -> "service/plate/",
    "ClassifyAnimal" -> "service/classify/dogorcat/",
    "MandarinASR" -> "service/asr/",
    "ClassifySentiment" -> "service/sentiment/classifier",
    "TextSegment" -> "service/text/segment/"
  )

  def getServiceUrl(name: String): String = {
    if (_aipmHttpHostUrl.endsWith("/")) {
      _aipmHttpHostUrl + servicesPath(name)
    }
    else {
      _aipmHttpHostUrl + "/" + servicesPath(name)
    }
  }

  //TODO: extract a common function with user-defined input/output parameters
  def computeFaceSimilarity(img1InputStream: InputStream, img2InputStream: InputStream): List[List[Double]] = {
    val serviceUrl = getServiceUrl("FaceSim")
    val contents = Map("image1" -> img1InputStream, "image2" -> img2InputStream)

    val res = WebUtils.doPost(serviceUrl, inStreamContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[List[List[Double]]]
    }
    else {
      null
    }
  }

  def extractFaceFeatures(img1InputStream: InputStream): List[List[Double]]= {
    val serviceUrl = getServiceUrl("FaceFeature")
    val contents = Map("image" -> img1InputStream)

    val res = WebUtils.doPost(serviceUrl, inStreamContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    if (map("res").asInstanceOf[Boolean]) {
      if(map("value").asInstanceOf[List[List[Double]]].length == 0){
//        throw new Exception("no face detected.")
          val arr = new Array[Double](128)
          List(arr.toList)

      } else {
        map("value").asInstanceOf[List[List[Double]]]
      }
    }
    else {
      null
    }
  }


  def extractPlateNumber(img1InputStream: InputStream): String = {
    val serviceUrl = getServiceUrl("PlateNumber")

    val contents = Map("image1" -> img1InputStream)

    val res = WebUtils.doPost(serviceUrl, inStreamContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[String]
    }
    else {
      ""
    }
  }

  def classifyAnimal(img1InputStream: InputStream): String = {
    val serviceUrl = getServiceUrl("ClassifyAnimal")

    val contents = Map("image1" -> img1InputStream)

    val res = WebUtils.doPost(serviceUrl, inStreamContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[String]
    }
    else {
      ""
    }

  }

  def mandarinASR(audio1InputStream: InputStream): String = {
    val serviceUrl = getServiceUrl("MandarinASR")
    val contents = Map("audio1" -> audio1InputStream)

    val res = WebUtils.doPost(serviceUrl, inStreamContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]
    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[String]
    }
    else {
      ""
    }
  }

  def sentimentClassifier(text: String): String = {
    val serviceUrl = getServiceUrl("ClassifySentiment")
    val contents = Map("text" -> text)

    val res = WebUtils.doPost(serviceUrl, strContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]

    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[String]
    }
    else {
      ""
    }
  }


  def segmentText(text: String): List[String] = {
    val serviceUrl = getServiceUrl("TextSegment")
    val contents = Map("text" -> text)

    val res = WebUtils.doPost(serviceUrl, strContents = contents)
    val json: Option[Any] = JSON.parseFull(res)
    val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]

    if (map("res").asInstanceOf[Boolean]) {
      map("value").asInstanceOf[List[String]]
    }
    else {
      Nil
    }
  }

}

/**
 */
trait ServiceInitializer extends PropertyValueComparator {
  var service: Services = null

  override def initialize(conf: Configuration): Unit = {
    val aipmHttpHostUrl = conf.getRequiredValueAsString("aipm.http.host.url")
    service = new Services(aipmHttpHostUrl)
  }

}