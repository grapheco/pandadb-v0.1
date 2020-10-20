import java.io.{File, FileInputStream}

import org.neo4j.blob.Blob
import cn.pandadb.database.PandaDB
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.Before
import org.neo4j.blob.impl.BlobFactory

class TestBase {
  @Before
  def setup(): Unit = {
//    setupNewDatabase()
    setupImageDatabase()
  }

  private def setupNewDatabase(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    if (true) {
      val db = openDatabase();
      val tx = db.beginTx();
      //create a node
      val node1 = db.createNode();

      node1.setProperty("name", "bob");
      node1.setProperty("age", 30);
      //property as a byte array
      node1.setProperty("bytes", IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))));
//      node1.setProperty("facephoto1", BlobFactory.fromFile(new File("./testinput/ai/face1.jpg")))
//      node1.setProperty("facephoto2", BlobFactory.fromFile(new File("./testinput/ai/face2.jpg")))

      //with a blob property
      node1.setProperty("photo", BlobFactory.fromFile(new File("./testinput/ai/test.png")));
      //blob array
      node1.setProperty("memo", (0 to 2).map(x => BlobFactory.fromFile(new File("./testinput/ai/test.txt"))).toArray);
      node1.setProperty("photo2", (0 to 1).map(x => BlobFactory.fromFile(new File("./testinput/ai/test.png"))).toArray);

      val node2 = db.createNode();

      node2.setProperty("name", "alex");
      //with a blob property
      node2.setProperty("photo", BlobFactory.fromFile(new File("./testinput/ai/test1.png")));

      tx.success();
      tx.close();
      db.shutdown();
    }
  }

  private def setupImageDatabase(): Unit ={
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    if (true) {
      val db = openDatabase();
      val tx = db.beginTx();

      val imageFileArr: Array[File] = _getFiles1(new File("./testinput/images"))
      for (i<-0 to imageFileArr.length-1) {
        val node = db.createNode()
        node.setProperty("name", i)
        node.setProperty("image", BlobFactory.fromFile(imageFileArr(i)))
      }
      tx.success()
      tx.close()
      db.shutdown()
    }
  }

  private def _getFiles1(dir: File): Array[File] = {
    dir.listFiles.filter(_.isFile) ++
      dir.listFiles.filter(_.isDirectory).flatMap(_getFiles1)
  }

  def openImageDatabase() =
    PandaDB.openDatabase(new File("./testoutput/testdb/data/databases/image/graph.db"),
      new File("./testinput/neo4j.conf"));

  def openDatabase() =
    PandaDB.openDatabase(new File("./testoutput/testdb/data/databases/graph.db"),
      new File("./testinput/neo4j.conf"));
}