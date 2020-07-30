package cn.pandadb.semop

import org.neo4j.blob.util.Configuration

trait SubPropertyExtractor {
  def declareProperties(): Map[String, Class[_]];

  def initialize(conf: Configuration);

  def extract(value: Any): Map[String, Any];
}

trait PropertyValueComparator {
  def initialize(conf: Configuration);
}

trait SingleValueComparator extends PropertyValueComparator {
  def compare(a: Any, b: Any): Double;
}

trait ValueSetComparator extends PropertyValueComparator {
  def compareAsSets(a: Any, b: Any): Array[Array[Double]];
}