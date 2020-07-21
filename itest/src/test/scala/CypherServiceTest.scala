import java.io.{File, FileInputStream}

import cn.pandadb.commons.blob.Blob
import cn.pandadb.database.{CypherService, PidbConnector}
import org.apache.commons.io.IOUtils
import org.junit.{Assert, Before, Test}
import org.neo4j.driver.v1.Record

class CypherServiceTest extends TestBase {
  @Before
  def setup(): Unit = {
    setupNewDatabase();
  }

  private def testCypher(client: CypherService): Unit = {
    //a non-blob
    val (node, name, age, bytes) = client.querySingleObject("match (n) where n.name='bob' return n, n.name, n.age, n.bytes", (result: Record) => {
      (result.get("n").asNode(), result.get("n.name").asString(), result.get("n.age").asInt(), result.get("n.bytes").asByteArray())
    });

    println(node.asMap());
    Assert.assertEquals(5, node.size());

    Assert.assertEquals("bob", name);
    Assert.assertEquals(30, age);
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))), bytes);

    //blob
    val blob0 = client.querySingleObject("return Blob.empty()", (result: Record) => {
      result.get(0).asBlob
    });

    Assert.assertEquals(0, blob0.length);

    val blob1 = client.querySingleObject("return Blob.fromFile('./test.png')", (result: Record) => {
      result.get(0).asBlob
    });

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
      blob1.offerStream {
        IOUtils.toByteArray(_)
      });

    client.querySingleObject("match (n) where n.name='bob' return n.photo,n.photo2", (result: Record) => {
      val blob2 = result.get("n.photo").asBlob;
      val blob22 = result.get("n.photo2").asList()
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
        blob2.offerStream {
          IOUtils.toByteArray(_)
        });

      Assert.assertEquals(2, blob22.size());

      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
        blob22.get(0).asInstanceOf[Blob].offerStream {
          IOUtils.toByteArray(_)
        });
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
        blob22.get(1).asInstanceOf[Blob].offerStream {
          IOUtils.toByteArray(_)
        });
    });

    client.querySingleObject("match (n) where n.name='alex' return n.photo", (result: Record) => {
      val blob3 = result.get("n.photo").asBlob
      Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test1.png"))),
        blob3.offerStream {
          IOUtils.toByteArray(_)
        });
    });

    //query with parameters
    val blob4 = client.querySingleObject("match (n) where n.name={NAME} return n.photo",
      Map("NAME" -> "bob"), (result: Record) => {
        result.get("n.photo").asBlob
      });

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
      blob4.offerStream {
        IOUtils.toByteArray(_)
      });

    //commit new records
    client.executeUpdate("CREATE (n {name:{NAME}})",
      Map("NAME" -> "张三"));

    client.executeUpdate("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      Map("NAME" -> "张三", "BLOB_OBJECT" -> Blob.EMPTY));

    client.executeUpdate("CREATE (n {name:{NAME}, photo:{BLOB_OBJECT}})",
      Map("NAME" -> "张三", "BLOB_OBJECT" -> Blob.fromFile(new File("./test1.png"))));

    client.executeQuery("return {BLOB_OBJECT}",
      Map("BLOB_OBJECT" -> Blob.fromFile(new File("./test.png"))));

    client.querySingleObject("return {BLOB_OBJECT}",
      Map("BLOB_OBJECT" -> Blob.fromFile(new File("./test.png"))), (result: Record) => {
        val blob = result.get(0).asBlob

        Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File("./test.png"))),
          blob.offerStream {
            IOUtils.toByteArray(_)
          });

      });
  }

  @Test
  def testRemoteBoltServer(): Unit = {
    val server = PidbConnector.startServer(new File("./testdb"), new File("./neo4j.conf"));
    val client = PidbConnector.connect("bolt://localhost:7687");

    testCypher(client);

    server.shutdown();
  }

  @Test
  def testLocalDB(): Unit = {
    val db = openDatabase();
    testCypher(PidbConnector.connect(db));
    db.shutdown();
  }
}