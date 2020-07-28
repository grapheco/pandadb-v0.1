import java.io.{File, FileInputStream}
import java.net.URL

import org.neo4j.blob.Blob
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.{Assert, Test}

class CypherPlusTest extends TestBase {
  @Test
  def testBlobLiteral(): Unit = {
    //create a new database
    val db = openDatabase()

    val blob1 = db.execute("return <https://www.baidu.com/img/flexible/logo/pc/result.png> as r").next().get("r").asInstanceOf[Blob];
    Assert.assertArrayEquals(IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob1.offerStream {
        IOUtils.toByteArray(_)
      })

    Assert.assertTrue(blob1.length > 0)

    val basedir = new File("./testinput/ai").getCanonicalFile.getAbsolutePath
    val blob2 = db.execute(s"return <file://${basedir}/bluejoe1.jpg> as r").next().get("r").asInstanceOf[Blob];
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File(basedir, "bluejoe1.jpg"))),
      blob2.offerStream {
        IOUtils.toByteArray(_)
      })

    Assert.assertTrue(blob2.length > 0)
    db.shutdown()
  }

  @Test
  def testLike(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();

    Assert.assertEquals(true, db.execute("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);
    Assert.assertEquals(true, db.execute("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);
    Assert.assertEquals(true, db.execute("return Blob.empty() ~:1.0 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute("return Blob.empty() ~: Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);

    val basedir = new File("./testinput/ai").getCanonicalFile.getAbsolutePath
    Assert.assertEquals(true, db.execute(
      s"return <file://${basedir}/bluejoe1.jpg> ~: <file://${basedir}/bluejoe2.jpg> as r")
      .next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute(s"<file://${basedir}/car1.jpg> ~: '.*730V7' as r")
      .next().get("r").asInstanceOf[Boolean]);

    tx.success();
    tx.close();
    db.shutdown();
  }

  @Test
  def testCompare(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();

    try {
      Assert.assertEquals(1.toLong, db.execute("return 1 :: 2 as r").next().get("r"));
      Assert.assertTrue(false);
    }
    catch {
      case t: Throwable =>
        t.printStackTrace()
        tx.failure()
        Assert.assertTrue(true);
    }

    val tx2 = db.beginTx();

    println(db.execute("return '沈志宏' :: '志宏 沈' as r").resultAsString())
    println(db.execute("return '沈志宏' ::jaro '志宏 沈' as r").resultAsString())
    println(db.execute("return '沈志宏' ::jaccard '志宏 沈' as r").resultAsString())
    println(db.execute("return '沈志宏' ::cosine '志宏 沈' as r").resultAsString())

    println(db.execute("return '沈志宏' ~: '志宏 沈' as r").resultAsString())
    println(db.execute("return '沈志宏' ~:jaro/0.7 '志宏 沈' as r").resultAsString())
    println(db.execute("return '沈志宏' ~:jaro/0.8 '志宏 沈' as r").resultAsString())

    Assert.assertTrue(db.execute("return '沈志宏' :: '志宏 沈' as r").next().get("r").asInstanceOf[Double] > 0.7);
    Assert.assertTrue(db.execute("return '沈志宏' :: '志宏 沈' as r").next().get("r").asInstanceOf[Double] < 0.8);
    Assert.assertTrue(db.execute("return '沈志宏' ::jaro '志宏 沈' as r").next().get("r").asInstanceOf[Double] > 0.7);
    Assert.assertEquals(true, db.execute("return '沈志宏' ~: '志宏 沈' as r").next().get("r"));
    Assert.assertEquals(true, db.execute("return '沈志宏' ~:jaro/0.7 '志宏 沈' as r").next().get("r"));
    Assert.assertEquals(false, db.execute("return '沈志宏' ~:jaro/0.8 '志宏 沈' as r").next().get("r"));

    tx2.success();
    tx2.close();
    db.shutdown();
  }

  @Test
  def testCustomProperty(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();
    /*
    Some(Query(None,SingleQuery(List(Return(false,ReturnItems(false,List(AliasedReturnItem(Property(FunctionInvocation(Namespace(List(Blob)),FunctionName(fromFile),false,Vector(StringLiteral(/Users/bluejoe/Pictures/test.wav))),PropertyKeyName(x)),Variable(x)))),None,None,None,None,Set())))))
    */
    val basedir = new File("./testinput/ai").getCanonicalFile.getAbsolutePath

    Assert.assertEquals(new File(basedir, "bluejoe1.jpg").length(),
      db.execute(s"return <file://${basedir}/bluejoe1.jpg> ->length as x")
        .next().get("x"));

    try {
      db.execute("""return <file://${basedir}/bluejoe1.jpg>->notExist""");
      Assert.assertTrue(false);
    }
    catch {
      case _: Throwable => Assert.assertTrue(true);
    }

    Assert.assertEquals("image/jpeg", db.execute(s"return <file://${basedir}/bluejoe1.jpg>->mime as x")
      .next().get("x"));

    Assert.assertEquals(3968, db.execute(s"return <file://${basedir}/bluejoe1.jpg>->width as x")
      .next().get("x"));

    Assert.assertEquals(2976, db.execute(s"return <file://${basedir}/bluejoe1.jpg>->height as x")
      .next().get("x"));

    Assert.assertEquals(true, db.execute(s"return <file://${basedir}/bluejoe1.jpg>->plateNumber = '京NB6666' as r")
      .next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute(s"return <file://${basedir}/test.wav>->message = '中华人民共和国' as r")
      .next().get("r").asInstanceOf[Boolean]);

    tx.success();
    tx.close();
    db.shutdown();
  }
}