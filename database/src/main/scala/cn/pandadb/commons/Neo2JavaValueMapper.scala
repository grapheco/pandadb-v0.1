package cn.pandadb.commons

import org.neo4j.internal.kernel.api.procs.Neo4jTypes
import org.neo4j.kernel.impl.proc.TypeMappers
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.values.AnyValue

import scala.collection.Iterable

/**
  * Created by bluejoe on 2018/12/11.
  */
class Neo2JavaValueMapper(typeMappers: TypeMappers) {
  def toNeo4jValue(value: Any): AnyValue = {
    ValueUtils.of(value);
  }

  def toJavaValue(value: AnyValue): Any = {
    value.map(typeMappers);
  }

  def toNeo4jType(javaClass: Class[_]): Neo4jTypes.AnyType = {
    typeMappers.toNeo4jType(javaClass);
  }

  /**
    * convert a scala object to an neo4j typed object
    *
    * @param x
    * @return
    */
  def scala2JavaValue(x: Any): Any = {
    x match {
      case m: Map[_, _]
      =>
        val jm = new java.util.HashMap[String, Any]();
        m.asInstanceOf[Map[String, Any]].map((kv) => (kv._1, scala2JavaValue(kv._2))).foreach((kv) => jm.put(kv._1, kv._2));
        jm

      case l: Iterable[_]
      => l.map((v) => scala2JavaValue(v)).toArray

      case a: Array[_]
      => a.map((v) => scala2JavaValue(v))

      case _ => x
    }
  }
}