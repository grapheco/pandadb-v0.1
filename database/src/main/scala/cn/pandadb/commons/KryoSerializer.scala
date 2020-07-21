package cn.pandadb.commons

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import org.objenesis.strategy.StdInstantiatorStrategy

import scala.collection.mutable

/**
  * Created by bluejoe on 2018/12/15.
  */
class KryoSerializer(classLoader: ClassLoader) {
  val kryoInstance = new Kryo();
  kryoInstance.setReferences(false);
  kryoInstance.setRegistrationRequired(false);
  kryoInstance.setClassLoader(classLoader);
  kryoInstance.setInstantiatorStrategy(new StdInstantiatorStrategy());

  def serialize(value: Any, os: OutputStream): Unit = {
    val output = new Output(os)
    output.clear();
    kryoInstance.writeClassAndObject(output, value);
    output.close();
  }

  def toBytes(value: Any): Array[Byte] = {
    val output = new ByteArrayOutputStream();
    serialize(value, output);
    output.toByteArray;
  }

  def deserialize(is: InputStream): Any = {
    val input = new Input(is);
    kryoInstance.readClassAndObject(input)
  }

  def fromBytes(bytes: Array[Byte]): Any = {
    deserialize(new ByteArrayInputStream(bytes));
  }
}

/**
  * static KryoSerializers
  */
object KryoSerializer {
  val _serializers = mutable.Map[String, KryoSerializer]();

  def of(name: String): KryoSerializer = {
    _serializers(name);
  }

  def bind(name: String, classLoader: ClassLoader) = {
    _serializers(name) = new KryoSerializer(classLoader);
  }
}
