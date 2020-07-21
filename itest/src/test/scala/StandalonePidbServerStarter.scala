import java.io.File

import cn.pandadb.database.PidbConnector
import org.apache.commons.io.FileUtils

object StandalonePidbServerStarter {
  def main(args: Array[String]) {
    //NOTE: setting working dir to ./pidb-engine in IDEA
    new TestBase().setupNewDatabase();
    PidbConnector.startServer(new File("./testdb"), new File("./neo4j.conf"));
  }
}