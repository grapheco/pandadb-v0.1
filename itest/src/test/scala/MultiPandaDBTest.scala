import java.io.{FileInputStream, File}

import org.neo4j.blob.Blob
import cn.pandadb.database.PandaDB
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.{Assert, Test}
import org.neo4j.graphdb.{Node, GraphDatabaseService}

class MultiPandaDBTest {
  def testCreateBlob(db: GraphDatabaseService, file: File): Unit = {
    val tx = db.beginTx();
    //create a node
    val node1 = db.createNode();

    node1.setProperty("name", "bob");
    node1.setProperty("age", 30);

    //with a blob property
    node1.setProperty("photo", Blob.fromFile(file));

    tx.success();
    tx.close();
  }

  def testQuery(db: GraphDatabaseService, file: File): Unit = {
    val tx = db.beginTx();

    //get first node
    val it = db.getAllNodes().iterator();
    val v1: Node = it.next();

    Assert.assertEquals(false, it.hasNext);
    Assert.assertEquals(3, v1.getAllProperties.size());

    val blob = v1.getProperty("photo").asInstanceOf[Blob];
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(file)),
      blob.offerStream {
        IOUtils.toByteArray(_)
      });

    tx.success();
    tx.close();
  }

  @Test
  def testMultiDbs(): Unit = {
    FileUtils.deleteDirectory(new File("./testdb1"));
    FileUtils.deleteDirectory(new File("./testdb2"));

    val db2 = PandaDB.openDatabase(new File("./testdb1"),
      new File("./neo4j.conf"));
    val db3 = PandaDB.openDatabase(new File("./testdb2"),
      new File("./neo4j.conf"));

    testCreateBlob(db2, new File("./testinput/test.png"))
    testCreateBlob(db3, new File("./testinput/test1.png"))
    testQuery(db2, new File("./testinput/test.png"))
    testQuery(db3, new File("./testinput/test1.png"))

    db2.shutdown()
    db3.shutdown()
  }
}