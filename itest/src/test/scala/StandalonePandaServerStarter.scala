import java.io.File

import cn.pandadb.database.PandaDB
import org.apache.commons.io.FileUtils

object StandalonePandaServerStarter {
  def main(args: Array[String]) {
    //NOTE: setting working dir to ./itest in IDEA
    //new TestBase().setupNewDatabase();
    PandaDB.startServer(new File("./testoutput/testdb"), new File("./neo4j.conf"));
  }
}