package cn.pandadb.database.blob.extensions

import cn.pandadb.commons.util.Logging
import cn.pandadb.database.BlobPropertyStoreService

import scala.collection.mutable.{Map => MMap}

/**
  * Created by bluejoe on 2018/8/12.
  */
class RuntimeContext extends Logging {
  private val _map = MMap[String, Any]();

  def contextPut[T](key: String, value: T): T = {
    _map(key) = value
    value
  };

  def contextPut[T](value: T)(implicit manifest: Manifest[T]): T = contextPut[T](manifest.runtimeClass.getName, value)

  def contextGet[T](key: String): T = {
    _map(key).asInstanceOf[T]
  };

  def contextGetOption[T](key: String): Option[T] = _map.get(key).map(_.asInstanceOf[T]);

  def contextGet[T]()(implicit manifest: Manifest[T]): T = contextGet(manifest.runtimeClass.getName);

  def contextGetOption[T]()(implicit manifest: Manifest[T]): Option[T] = contextGetOption(manifest.runtimeClass.getName);

  def getBlobPropertyStoreService: BlobPropertyStoreService = contextGet[BlobPropertyStoreService];
}