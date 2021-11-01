import java.io.File

import cn.pandadb.database.PandaDB
import cn.pandadb.server.PandaServer
import org.apache.commons.io.FileUtils

object StandalonePandaServerForTest {
  def main(args: Array[String]) {
    //NOTE: setting working dir to ./itest in IDEA
    //new TestBase().setup();
    PandaServer.start(new File("./testinput"),
      new File("./testinput/neo4j.conf"));
  }
}