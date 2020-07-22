import java.io.File

import cn.pandadb.commons.blob.Blob
import cn.pandadb.database.PandaDB
import org.apache.commons.io.FileUtils

object BlobIOPerfTest {
  def main(args: Array[String]) {
    if (args.length < 3)
      throw new RuntimeException("BlobIOPerfTest <pidb-dir> <pidb-conf-file> <blob-numbers>");

    val dir = args(0);
    val conf = args(1);
    val n: Int = args(2).toInt;

    println(s"dir: $dir, conf: $conf, number: $n");
    FileUtils.deleteDirectory(new File(dir));
    val db = PandaDB.openDatabase(new File(dir), new File(conf));

    println("start inserting blobs...");
    val start = System.currentTimeMillis();

    var x = 0;
    for (i <- 1 to n) {
      val tx = db.beginTx();

      val node = db.createNode();
      node.setProperty("id", i);
      //with a blob property
      for (m <- 0 to 9)
        node.setProperty(s"photo1-$m", Blob.fromFile(new File("./test.png")));

      for (m <- 0 to 9)
        node.setProperty(s"photo2-$m", (0 to 9).map(x => Blob.fromFile(new File("./test.png"))).toArray);

      x += (10 + 10 * 10)

      if (i % 100 == 0)
        println(s"nodes: $i, blobs: $x");

      tx.success();
      tx.close();
    }

    val end = System.currentTimeMillis();
    val elapse = end - start;
    println(elapse);
    println(s"each node: ${elapse * 1.0 / n}ms, each blob: ${elapse * 1.0 / x}ms");
    db.shutdown();
  }
}