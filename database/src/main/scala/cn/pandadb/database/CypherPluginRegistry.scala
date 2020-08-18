package cn.pandadb.database

import cn.pandadb.semop.{DomainType, PropertyValueComparator, SemanticComparator, SemanticExtractor, SingleValueComparator, SubPropertyExtractor, ValueSetComparator}
import org.neo4j.blob.util.{Configuration, Logging}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory
import org.reflections.Reflections

import scala.beans.BeanProperty
import scala.collection.mutable
import org.neo4j.blob.util.ConfigUtils._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * Created by bluejoe on 2019/1/31.
 */
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

case class DomainExtractorEntry(extractor: SubPropertyExtractor,
                                name: String,
                                domain: DomainType) {
}

case class DomainComparatorEntry(comparator: PropertyValueComparator,
                                 name: String,
                                 domain: (DomainType, DomainType),
                                 threshold: Double) {
}

class CypherPluginRegistry(conf: Configuration) extends Logging {
  val extractors = ArrayBuffer[DomainExtractorEntry]();
  val comparators = ArrayBuffer[DomainComparatorEntry]();

  {
    val packages = conf.getValueAsString("semops.package.prefix", "cn.pandadb.semoplib").split(";")

    packages.foreach { prefix =>
      val reflections = new Reflections(prefix)
      reflections.getTypesAnnotatedWith(classOf[SemanticExtractor]).toSet.foreach { clz: Class[_] =>
        val an = clz.getAnnotation(classOf[SemanticExtractor])
        extractors += DomainExtractorEntry(
          clz.newInstance().asInstanceOf[SubPropertyExtractor],
          an.name(),
          an.domain())
      }

      reflections.getTypesAnnotatedWith(classOf[SemanticComparator]).toSet.foreach { clz: Class[_] =>
        val an = clz.getAnnotation(classOf[SemanticComparator])
        comparators += DomainComparatorEntry(
          clz.newInstance().asInstanceOf[PropertyValueComparator],
          an.name(),
          an.domains()(0) -> an.domains()(1),
          an.threshold())
      }
    }

    if (!extractors.isEmpty && logger.isDebugEnabled()) {
      logger.debug(s"loaded ${extractors.size} semantic extractors: ${extractors.map(_.name).toList}")
    }

    if (!comparators.isEmpty && logger.isDebugEnabled()) {
      logger.debug(s"loaded ${comparators.size} semantic comparators: ${comparators.map(_.name).toList}")
    }
  }

  private def domainTypeNameOf(x: Any): DomainType = x match {
    case b: Blob => b.mimeType.major.toLowerCase() match {
      case "image" => DomainType.BlobImage
      case "audio" => DomainType.BlobAudio
    }
    case _: String => DomainType.String
  }

  def createCustomPropertyProvider() = new CustomPropertyProvider {
    extractors.foreach(_.extractor.initialize(conf));

    //propertyName, typeName
    val map: Map[(String, DomainType), Array[SubPropertyExtractor]] = extractors
      .flatMap(x => x.extractor.declareProperties().map(prop => (prop._1, x.domain) -> x.extractor))
      .groupBy(_._1)
      .map(x => x._1 -> x._2.map(_._2).toArray)

    //TODO: cache extraction
    def getCustomProperty(x: Any, name: String): Option[Any] = {
      //image:png
      val m1 = map.get(name -> domainTypeNameOf(x)).map(_.head.extract(x).apply(name));

      if (m1.isDefined) {
        m1
      }
      else {
        //blob:*
        val m3 =
          if (x.isInstanceOf[Blob]) {
            map.get(name -> DomainType.BlobAny).map(_.head.extract(x).apply(name))
          }
          else {
            None
          }

        if (m3.isDefined) {
          m3
        }
        else {
          //*
          map.get(name -> DomainType.Any)
            .map(_.head.extract(x).apply(name))
        }
      }
    }
  }

  def createValueComparatorRegistry() = new ValueMatcher with Logging {
    type CompareAnyMethod = (Any, Any) => Any;
    type CompareValueMethod = (Any, Any) => Double;
    type CompareSetMethod = (Any, Any) => Array[Array[Double]];

    //initialize all comparators
    comparators.map(_.comparator).foreach(_.initialize(conf));

    def like(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean] = {
      (a, b) match {
        case (null, null) => Some(true)
        case (BlobFactory.EMPTY, BlobFactory.EMPTY) => Some(true)
        case (null, _) => Some(false)
        case (_, null) => Some(false)
        case _ =>
          val (m: CompareValueMethod, e: DomainComparatorEntry) = getNotNullValueComparator(a, b, algoName)
          Some(m.apply(a, b) > threshold.getOrElse(e.threshold))
      }
    }

    override def compareOne(a: Any, b: Any, algoName: Option[String]): Option[Double] = {
      (a, b) match {
        case (null, null) => Some(1.0)
        case (null, _) => Some(0.0)
        case (_, null) => Some(0.0)
        case _ =>
          val (m: CompareValueMethod, e: DomainComparatorEntry) = getNotNullValueComparator(a, b, algoName)
          Some(m.apply(a, b))
      }
    }

    override def compareSet(a: Any, b: Any, algoName: Option[String]): Option[Array[Array[Double]]] = {
      (a, b) match {
        case (null, null) => Some(Array(Array(1.0)))
        case (null, _) => Some(Array(Array(0.0)))
        case (_, null) => Some(Array(Array(0.1)))
        case _ =>
          val (m: CompareSetMethod, e: DomainComparatorEntry) = getNotNullSetComparator(a, b, algoName)
          Some(m.apply(a, b))
      }
    }

    override def containsOne(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean] = {
      (a, b) match {
        case (null, null) => Some(true)
        case (null, _) => Some(false)
        case (_, null) => Some(true)
        case _ =>
          val (m: CompareValueMethod, e: DomainComparatorEntry) = getNotNullValueComparator(a, b, algoName)
          val r = m.apply(a, b)
          val th = threshold.getOrElse(e.threshold)
          Some(r > th)
      }
    }

    override def containsSet(a: Any, b: Any, algoName: Option[String], threshold: Option[Double]): Option[Boolean] = {
      (a, b) match {
        case (null, null) => Some(true)
        case (null, _) => Some(false)
        case (_, null) => Some(true)
        case _ =>
          val (m: CompareSetMethod, e: DomainComparatorEntry) = getNotNullSetComparator(a, b, algoName)
          val r = m.apply(a, b)
          val th = threshold.getOrElse(e.threshold)
          Some(!r.exists(_.max <= th))
      }
    }

    private def getMatchedComparator(compareValueOrSet: Boolean, typeA: DomainType, typeB: DomainType, algoName: Option[String]): Option[(CompareAnyMethod, DomainComparatorEntry)] = {
      def isEntryMatched(entry: DomainComparatorEntry, typeA: DomainType, typeB: DomainType): Boolean = {
        val f = entry.domain.equals(typeA -> typeB) &&
          (if (compareValueOrSet)
            entry.comparator.isInstanceOf[SingleValueComparator]
          else
            entry.comparator.isInstanceOf[ValueSetComparator]
            )

        algoName.map { name =>
          f && entry.name.equalsIgnoreCase(name)
        }.getOrElse {
          f
        }
      }

      def doCompare(comparator: PropertyValueComparator, a: Any, b: Any): Any = {
        if (compareValueOrSet)
          comparator.asInstanceOf[SingleValueComparator].compare(a, b);
        else
          comparator.asInstanceOf[ValueSetComparator].compareAsSets(a, b);
      }

      comparators.find(isEntryMatched(_, typeA, typeB))
        .map(entry => ((x: Any, y: Any) => doCompare(entry.comparator, x, y)) -> entry)
        .orElse(comparators.find(isEntryMatched(_, typeB, typeA))
          .map(entry => ((x: Any, y: Any) => doCompare(entry.comparator, y, x)) -> entry))
    }

    val _cachedValueComparators = mutable.Map[(DomainType, DomainType, Option[String]), Option[(CompareValueMethod, DomainComparatorEntry)]]();
    val _cachedSetComparators = mutable.Map[(DomainType, DomainType, Option[String]), Option[(CompareSetMethod, DomainComparatorEntry)]]();

    private def getNotNullValueComparator(a: Any, b: Any, algoName: Option[String]): (CompareValueMethod, DomainComparatorEntry) = {
      val typeA = domainTypeNameOf(a)
      val typeB = domainTypeNameOf(b)
      _cachedValueComparators.getOrElseUpdate((typeA, typeB, algoName), {
        val opt = getMatchedComparator(compareValueOrSet = true, typeA, typeB, algoName);
        opt.map((x) => x._1.asInstanceOf[CompareValueMethod] -> x._2)
          .orElse(
            getMatchedComparator(compareValueOrSet = false, typeA, typeB, algoName)
              .map(en => asCompareValueMethod(en._1.asInstanceOf[CompareSetMethod]) -> en._2))
      }
      ).getOrElse(throw new NoSuitableComparatorException(typeA, typeB, algoName))
    }

    private def getNotNullSetComparator(a: Any, b: Any, algoName: Option[String]): (CompareSetMethod, DomainComparatorEntry) = {
      val typeA = domainTypeNameOf(a)
      val typeB = domainTypeNameOf(b)
      _cachedSetComparators.getOrElseUpdate((typeA, typeB, algoName), {
        val opt = getMatchedComparator(compareValueOrSet = false, typeA, typeB, algoName);
        opt.map((x) => x._1.asInstanceOf[CompareSetMethod] -> x._2)
          .orElse(
            getMatchedComparator(compareValueOrSet = true, typeA, typeB, algoName)
              .map(en => asCompareSetMethod(en._1.asInstanceOf[CompareValueMethod]) -> en._2))
      }
      ).getOrElse(throw new NoSuitableComparatorException(typeA, typeB, algoName))
    }

    private def asCompareValueMethod(m: CompareSetMethod): CompareValueMethod = {
      (a: Any, b: Any) =>
        val r: Array[Array[Double]] = m(a, b);
        r.flatMap(x => x).max;
    }

    private def asCompareSetMethod(m: CompareValueMethod): CompareSetMethod = {
      (a: Any, b: Any) =>
        val r: Double = m(a, b);
        Array(Array(r))
    }
  }
}

class UnknownPropertyException(name: String, x: Any)
  extends RuntimeException(s"unknown property `$name` for $x") {

}

class NoSuitableComparatorException(a: DomainType, b: DomainType, algoName: Option[String])
  extends RuntimeException(s"no suitable comparator: ${a.name()} and ${b.name}, algorithm name: ${algoName.getOrElse("(none)")}") {

}