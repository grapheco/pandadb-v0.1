package cn.pandadb.semoplib.text

import cn.pandadb.semop.{DomainType, SemanticComparator, SingleValueComparator}
import info.debatty.java.stringsimilarity.{Cosine, Jaccard, JaroWinkler}
import org.neo4j.blob.util.Configuration

/**
 * Created by bluejoe on 2019/2/17.
 */
@SemanticComparator(name = "jaro", domains = Array(DomainType.String, DomainType.String), threshold = 0.6)
class JaroWinklerStringSimilarity extends SingleValueComparator {
  def compare(str1: Any, str2: Any): Double = {
    val jw = new JaroWinkler();
    jw.similarity(str1.asInstanceOf[String], str2.asInstanceOf[String]);
  }

  override def initialize(conf: Configuration): Unit = {

  }
}

@SemanticComparator(name = "jaccard", domains = Array(DomainType.String, DomainType.String), threshold = 0.6)
class JaccardStringSimilarity extends SingleValueComparator {
  def compare(str1: Any, str2: Any): Double = {
    val jw = new Jaccard();
    jw.similarity(str1.asInstanceOf[String], str2.asInstanceOf[String]);
  }

  override def initialize(conf: Configuration): Unit = {

  }
}

@SemanticComparator(name = "cosine", domains = Array(DomainType.String, DomainType.String), threshold = 0.6)
class CosineStringSimilarity extends SingleValueComparator {
  def compare(str1: Any, str2: Any): Double = {
    val jw = new Cosine();
    jw.similarity(str1.asInstanceOf[String], str2.asInstanceOf[String]);
  }

  override def initialize(conf: Configuration): Unit = {

  }
}