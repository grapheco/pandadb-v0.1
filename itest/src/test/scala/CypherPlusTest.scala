import java.io.File

import cn.pandadb.database.PidbConnector
import org.apache.commons.io.FileUtils
import org.junit.{Assert, Test}

class CypherPlusTest {
  @Test
  def testLike(): Unit = {
    FileUtils.deleteDirectory(new File("./testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();

    Assert.assertEquals(true, db.execute("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);
    Assert.assertEquals(true, db.execute("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);
    Assert.assertEquals(true, db.execute("return Blob.empty() ~:1.0 Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute("return Blob.empty() ~: Blob.empty() as r").next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute(
      """return Blob.fromFile('/Users/bluejoe/Pictures/similarity_test_1.png')
      ~: Blob.fromFile('/Users/bluejoe/Pictures/similarity_test_2.png') as r""")
      .next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg') ~: '.*NB666.*' as r""")
      .next().get("r").asInstanceOf[Boolean]);

    tx.success();
    tx.close();
    db.shutdown();
  }

  @Test
  def testCompare(): Unit = {
    FileUtils.deleteDirectory(new File("./testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();

    try {
      Assert.assertEquals(1.toLong, db.execute("return 1 :: 2 as r").next().get("r"));
      Assert.assertTrue(false);
    }
    catch {
      case _:Throwable => Assert.assertTrue(true);
    }

    Assert.assertEquals(true, db.execute("return Blob.fromFile('/Users/bluejoe/Pictures/meng.jpg') :: Blob.fromFile('/Users/bluejoe/Pictures/event.jpg') as r").next().get("r").asInstanceOf[Double] > 0.7);
    Assert.assertEquals(true, db.execute("return Blob.fromFile('/Users/bluejoe/Pictures/simba.jpg') :: Blob.fromFile('/Users/bluejoe/Pictures/simba2.jpg') as r").next().get("r").asInstanceOf[Double] > 0.9);
    Assert.assertEquals(0.9, db.execute("return '杜 一' :: '杜一' as r").next().get("r"));
    Assert.assertEquals(0.9, db.execute("return '杜 一' ::jaro '杜一' as r").next().get("r"));
    Assert.assertEquals(0.75, db.execute("return 'Yi Du' :: 'DU Yi' as r").next().get("r"));

    db.execute("return '杜 一' ::jaro '杜一','Zhihong SHEN' ::levenshtein 'SHEN Z.H'");

    tx.success();
    tx.close();
    db.shutdown();
  }

  @Test
  def testCustomProperty(): Unit = {
    FileUtils.deleteDirectory(new File("./testdb"));
    //create a new database
    val db = openDatabase();
    val tx = db.beginTx();
    /*
    Some(Query(None,SingleQuery(List(Return(false,ReturnItems(false,List(AliasedReturnItem(Property(FunctionInvocation(Namespace(List(Blob)),FunctionName(fromFile),false,Vector(StringLiteral(/Users/bluejoe/Pictures/test.wav))),PropertyKeyName(x)),Variable(x)))),None,None,None,None,Set())))))
    */
    Assert.assertEquals(new File("/Users/bluejoe/Pictures/1.jpeg").length(),
      db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->length as x""")
        .next().get("x"));

    try {
      db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->notExist""");
      Assert.assertTrue(false);
    }
    catch {
      case _:Throwable => Assert.assertTrue(true);
    }

    Assert.assertEquals("image/jpeg", db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->mime as x""")
      .next().get("x"));

    Assert.assertEquals(1, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->test1 as x""")
      .next().get("x"));

    Assert.assertEquals("hello", db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->test2 as x""")
      .next().get("x"));

    Assert.assertEquals(500, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->width as x""")
      .next().get("x"));

    Assert.assertEquals(635, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->height as x""")
      .next().get("x"));

    Assert.assertEquals(true, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/1.jpeg')->plateNumber = '京NB6666' as r""")
      .next().get("r").asInstanceOf[Boolean]);

    Assert.assertEquals(true, db.execute("""return Blob.fromFile('/Users/bluejoe/Pictures/test.wav')->message = '中华人民共和国' as r""")
      .next().get("r").asInstanceOf[Boolean]);

    tx.success();
    tx.close();
    db.shutdown();
  }

  def openDatabase() = PidbConnector.openDatabase(new File("./testdb/data/databases/graph.db"),
    new File("./neo4j.conf"));
}