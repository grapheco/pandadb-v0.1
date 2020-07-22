package cn.pandadb.query

import cn.pandadb.commons.blob.Blob
import org.neo4j.cypher.internal.frontend.v3_4.parser.BlobURL

trait BlobFactory {
  def createBlob(url: BlobURL): Blob;
}

trait CustomPropertyProvider {
  def getCustomProperty(x: Any, propertyName: String): Option[Any];
}

trait ValueMatcher {
  def like(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean];

  def containsOne(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean];

  def containsSet(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean];

  /**
   * compares two values
   */
  def compareOne(a: Any, b: Any, algoName: Option[String]): Option[Double];

  /**
   * compares two objects as sets
   */
  def compareSet(a: Any, b: Any, algoName: Option[String]): Option[Array[Array[Double]]];
}