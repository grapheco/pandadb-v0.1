import java.io.{File, FileInputStream}
import java.util

import cn.pandadb.database.PandaDB
import org.apache.commons.io.FileUtils
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory

class BlobFuncTest extends TestBase {
  @Test
  def test2(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();

    Assert.assertEquals(0, db.execute("return Blob.empty() as blob").next().get("blob")
      .asInstanceOf[Blob].length);

    Assert.assertEquals(0, db.execute("return Blob.len(Blob.empty()) as len").next().get("len").asInstanceOf[Long]);
    Assert.assertArrayEquals("hello world".getBytes("utf-8"),
      db.execute("return Blob.fromUTF8String('hello world') as x").next().get("x").asInstanceOf[Blob].toBytes());
    Assert.assertEquals("hello world",
      db.execute("return Blob.toUTF8String(Blob.fromUTF8String('hello world')) as x").next().get("x").asInstanceOf[String]);

    //create a node
    val node1 = db.createNode();
    node1.setProperty("name", "bob");
    //with a blob property
    node1.setProperty("photo", BlobFactory.fromFile(new File("./testinput/ai/test.png")));
    db.execute("create (n: Person {name:'yahoo', photo: Blob.fromFile('./testinput/ai/test2.jpg')})");
    db.execute("create (n: Person {name:'yahoo', photo: Blob.fromFile('./testinput/ai/test2.jpg')} return n)");
    val len2 = db.execute("return Blob.len(Blob.fromFile('./testinput/ai/test.png')) as len").next().get("len").asInstanceOf[Long];
    Assert.assertEquals(len2, new File("./testinput/ai/test.png").length());

    val len = db.execute("match (n) where n.name='bob' return Blob.len(n.photo) as len").next().get("len").asInstanceOf[Long];
    Assert.assertEquals(len, new File("./testinput/ai/test.png").length());

    val result: util.Map[String, AnyRef] = db.execute(
      """
      match (n) where n.name='yahoo'
      return Blob.len(n.photo) as len,
      Blob.mime(n.photo) as mimetype,
      Blob.mime1(n.photo) as majormime,
      Blob.mime2(n.photo) as minormime
      """).next();

    Assert.assertEquals(new File("./testinput/ai/test2.jpg").length(), result.get("len").asInstanceOf[Long]);
    Assert.assertEquals("image/jpeg", result.get("mimetype").asInstanceOf[String]);
    Assert.assertEquals("image", result.get("majormime").asInstanceOf[String]);
    Assert.assertEquals("jpeg", result.get("minormime").asInstanceOf[String]);

    Assert.assertEquals(1, db.execute("match (n) where Blob.mime(n.photo)='image/png' return n").stream().count());
    Assert.assertEquals(2, db.execute("match (n) where Blob.mime1(n.photo)='image' return n").stream().count());

    tx.success();
    tx.close();
    db.shutdown();
  }
}