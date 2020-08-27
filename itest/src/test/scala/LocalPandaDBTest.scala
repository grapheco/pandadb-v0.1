import java.io.{File, FileInputStream}

import org.neo4j.blob.Blob
import org.apache.commons.io.IOUtils
import org.junit.{Assert, Before, Test}
import org.neo4j.blob.impl.BlobFactory
import org.neo4j.graphdb.Node

import scala.collection.JavaConversions

class LocalPandaDBTest extends TestBase {
  @Test
  def testAPI(): Unit = {
    //reload database
    val db2 = openDatabase();
    val tx2 = db2.beginTx();

    //get first node
    val it = db2.getAllNodes().iterator();
    val v1: Node = it.next();
    val v2: Node = it.next();

    println(v1.getAllProperties);
    Assert.assertEquals(6, v1.getAllProperties.size());

    val photo = v1.getProperty("photo").asInstanceOf[Blob];
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      photo.offerStream {
        IOUtils.toByteArray(_)
      });

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      v1.getProperty("bytes").asInstanceOf[Array[Byte]]);

    //test array[blob]
    val memo = v1.getProperty("memo").asInstanceOf[Array[Blob]];
    Assert.assertEquals(3, memo.length);
    Assert.assertEquals(IOUtils.toString(new FileInputStream(new File("./testinput/ai/test.txt"))),
      memo(0).offerStream {
        IOUtils.toString(_)
      })

    Assert.assertEquals(IOUtils.toString(new FileInputStream(new File("./testinput/ai/test.txt"))),
      memo(1).offerStream {
        IOUtils.toString(_)
      })

    Assert.assertEquals(IOUtils.toString(new FileInputStream(new File("./testinput/ai/test.txt"))),
      memo(2).offerStream {
        IOUtils.toString(_)
      })

    val photos = v1.getProperty("photo2").asInstanceOf[Array[Blob]];
    Assert.assertEquals(2, photos.length);
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      photos(0).offerStream {
        IOUtils.toByteArray(_)
      });

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      photos(1).offerStream {
        IOUtils.toByteArray(_)
      });

    //delete one
    v1.removeProperty("photo");
    //photo will be deleted
    try {
      photo.offerStream(_.read())
      Assert.assertTrue(false)
    }
    catch {
      case _=>
        Assert.assertTrue(true)
    }

    v1.removeProperty("memo")
    try {
      memo(0).offerStream(_.read())
      Assert.assertTrue(false)
    }
    catch {
      case _=>
        Assert.assertTrue(true)
    }

    v1.removeProperty("photos");
    try {
      photos(0).offerStream(_.read())
      Assert.assertTrue(false)
    }
    catch {
      case _=>
        Assert.assertTrue(true)
    }

    v2.delete();

    val it2 = db2.getAllNodes().iterator();
    it2.next();
    Assert.assertEquals(false, it2.hasNext);

    tx2.success();
    tx2.close()
    //TODO: OnlineIndexUpdates will fail on load deleted blob value!
    db2.shutdown();
  }

  @Test
  def testCypherQuery(): Unit = {
    //reload database
    val db2 = openDatabase();
    val tx2 = db2.beginTx();

    //cypher query
    val r1 = db2.execute("match (n) where n.name='bob' return n.photo,n.name,n.age,n.bytes,n.photo2").next();
    Assert.assertEquals("bob", r1.get("n.name"));
    Assert.assertEquals(30, r1.get("n.age"))

    Assert.assertArrayEquals(
      IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      r1.get("n.bytes").asInstanceOf[Array[Byte]])

    val blob22 = r1.get("n.photo2").asInstanceOf[Array[Blob]];
    Assert.assertEquals(2, blob22.length);
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      blob22(0).offerStream {
        IOUtils.toByteArray(_)
      });

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      blob22(1).offerStream {
        IOUtils.toByteArray(_)
      });

    val blob1 = r1.get("n.photo").asInstanceOf[Blob];

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      blob1.offerStream {
        IOUtils.toByteArray(_)
      });

    val blob3 = db2.execute("match (n) where n.name='alex' return n.photo").next()
      .get("n.photo").asInstanceOf[Blob];

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test1.png"))),
      blob3.offerStream {
        IOUtils.toByteArray(_)
      });

    tx2.success();
    tx2.close();
    db2.shutdown();
  }

  @Test
  def testCypherCreate(): Unit = {
    //reload database
    val db2 = openDatabase();
    val tx2 = db2.beginTx();

    db2.execute("CREATE (n {name:{NAME}})",
      JavaConversions.mapAsJavaMap(Map("NAME" -> "张三")));

    db2.execute("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      JavaConversions.mapAsJavaMap(Map("NAME" -> "张三", "BLOB_OBJECT" -> BlobFactory.EMPTY)));

    db2.execute("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      JavaConversions.mapAsJavaMap(Map("NAME" -> "张三", "BLOB_OBJECT" -> BlobFactory.fromFile(new File("./testinput/ai/test.png")))));

    Assert.assertEquals(3.toLong, db2.execute("match (n) where n.name=$NAME return count(n)",
      JavaConversions.mapAsJavaMap(Map("NAME" -> "张三"))).next().get("count(n)"));

    val it2 = db2.execute("match (n) where n.name=$NAME return n.photo",
      JavaConversions.mapAsJavaMap(Map("NAME" -> "张三")));

    Assert.assertEquals(null,
      it2.next().get("n.photo"));

    Assert.assertEquals(it2.next().get("n.photo"), BlobFactory.EMPTY);

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
      it2.next().get("n.photo").asInstanceOf[Blob].offerStream {
        IOUtils.toByteArray(_)
      });

    tx2.success();
    tx2.close();
    db2.shutdown();
  }
}