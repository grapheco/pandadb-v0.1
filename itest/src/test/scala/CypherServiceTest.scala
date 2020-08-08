import java.io.{File, FileInputStream}

import org.neo4j.blob.Blob
import cn.pandadb.connector.{CypherService, LocalGraphService, RemotePandaServer}
import cn.pandadb.server.PandaServer
import org.apache.commons.io.IOUtils
import org.junit.{Assert, Test}
import org.neo4j.blob.impl.BlobFactory
import org.neo4j.driver.Record

class CypherServiceTest extends TestBase {
  private def testCypher(client: CypherService): Unit = {
    //a non-blob
    val (node, name, age, bytes) = client.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age, n.bytes", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt(), result.get("n.bytes").asByteArray())
    });

    println(node.asMap());
    Assert.assertEquals(5, node.size());
    Assert.assertEquals("bob", name);
    Assert.assertEquals(30, age);
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))), bytes);
    Assert.assertEquals(41745, node.get("photo").asBlob().length);
    Assert.assertEquals("image/png", node.get("photo").asBlob().mimeType.text);

    val basedir = new File("./testinput").getAbsoluteFile.getCanonicalPath

    //blob
    val blob0 = client.querySingleObject("return Blob.empty()", (result: Record) => {
      result.get(0).asBlob
    });

    Assert.assertEquals(0, blob0.length);

    val blob1 = client.querySingleObject(s"return Blob.fromFile('${basedir}/ai/test.png')", (result: Record) => {
      val onlineBlob = result.get(0).asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        onlineBlob.offerStream {
          IOUtils.toByteArray(_)
        });
    });

    client.querySingleObject(s"return <file://${basedir}/ai/test.png>", (result: Record) => {
      val onlineBlob = result.get(0).asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        onlineBlob.offerStream {
          IOUtils.toByteArray(_)
        })
    })

    client.querySingleObject(s"CREATE (n {name:'yahoo', photo:<file://${basedir}/ai/test.png>}) return n.photo", (result: Record) => {
      val onlineBlob = result.get(0).asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        onlineBlob.offerStream {
          IOUtils.toByteArray(_)
        })
    })

    client.querySingleObject(s"CREATE (n {name:'lianxin', photo:<file://./testinput/ai/test.png>}) return n", (result: Record) => {
      val onlineBlob = result.get(0).asNode().get("photo").asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        onlineBlob.offerStream {
          IOUtils.toByteArray(_)
        })
    })

    client.querySingleObject("match (n) where n.name='bob' return n.photo,n.photo2", (result: Record) => {
      val blob1 = result.get("n.photo").asBlob;
      val blob2 = result.get("n.photo2").asList()
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        blob1.offerStream {
          IOUtils.toByteArray(_)
        });

      Assert.assertEquals(2, blob2.size());

      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        blob2.get(0).asInstanceOf[Blob].offerStream {
          IOUtils.toByteArray(_)
        });
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
        blob2.get(1).asInstanceOf[Blob].offerStream {
          IOUtils.toByteArray(_)
        });
    });

    client.querySingleObject("match (n) where n.name='alex' return n.photo", (result: Record) => {
      val onlineBlob = result.get("n.photo").asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test1.png"))),
        onlineBlob.offerStream {
          IOUtils.toByteArray(_)
        });
    });

    //query with parameters
    client.querySingleObject("match (n) where n.name={NAME} return n.photo",
      Map("NAME" -> "bob"), (result: Record) => {
        val onlineBlob = result.get("n.photo").asBlob
        Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
          onlineBlob.offerStream {
            IOUtils.toByteArray(_)
          });
      });

    //commit new records
    client.executeUpdate("CREATE (n {name:{NAME}})",
      Map("NAME" -> "张三"));

    client.executeUpdate("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      Map("NAME" -> "张三", "BLOB_OBJECT" -> BlobFactory.EMPTY));

    client.executeUpdate("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      Map("NAME" -> "张三", "BLOB_OBJECT" -> BlobFactory.fromFile(new File("./testinput/ai/test1.png"))));

    val res = client.queryObjects("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}}) return n.photo",
      Map("NAME" -> "张三", "BLOB_OBJECT" -> BlobFactory.fromFile(new File("./testinput/ai/test1.png"))),
      (record) => {
        val blob = record.get(0).asBlob()
        Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test1.png"))),
          blob.offerStream {
            IOUtils.toByteArray(_)
          });
      });

    Assert.assertEquals(1, res.length)

    client.executeQuery("return {BLOB_OBJECT}",
      Map("BLOB_OBJECT" -> BlobFactory.fromFile(new File("./testinput/ai/test.png"))), (result) => result);

    client.querySingleObject("return {BLOB_OBJECT}",
      Map("BLOB_OBJECT" -> BlobFactory.fromFile(new File("./testinput/ai/test.png"))), (result: Record) => {
        val blob = result.get(0).asBlob

        Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))),
          blob.offerStream {
            IOUtils.toByteArray(_)
          });

      });
  }

  @Test
  def testRemoteBoltServer(): Unit = {
    val server = PandaServer.start(new File("./testoutput/testdb"), new File("./testinput/neo4j.conf"), Map("dbms.connector.http.enabled" -> "false"));
    val client = RemotePandaServer.connect("bolt://localhost:7687");
    testCypher(client);
    server.shutdown();
  }

  @Test
  def testLocalDB(): Unit = {
    val db = openDatabase();
    testCypher(LocalGraphService.connect(db));
    db.shutdown();
  }
}