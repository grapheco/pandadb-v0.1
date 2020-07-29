package cn.panda.semop.test
import org.neo4j.blob.util.Configuration

class ConfigTemp extends Configuration {
  override def getRaw(name: String): Option[String] = {
    val configs = Map("aipm.http.host.url"->"http://10.0.86.128:8081/")
    configs.get(name)
  }
}

class TestBase {
  val config = new ConfigTemp()
}
