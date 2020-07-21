package BatchFlushTest
import java.io.File

import cn.pandadb.commons.blob.Blob
import cn.pandadb.database.PidbConnector
import org.apache.commons.io.FileUtils
object PutHBase {
  def main(args: Array[String]): Unit = {
    val dir = "target/tmp"
    val n: Int = 1

    println(s"dir: $dir, number: $n")
    FileUtils.deleteDirectory(new File(dir))
    val db = PidbConnector.openDatabase(new File(dir), new File("pidb-engine/neo4j-hbase.conf"));

    println("start inserting blobs...")
    val start = System.currentTimeMillis()
    for (i <- 0 until n) {
      val tx = db.beginTx()
      for (j <- 0 until 1000) {
        val node = db.createNode()
        node.setProperty("id", j)
        //with a blob property
        node.setProperty("photo", Blob.fromFile(new File("pidb-engine/test.png")));
      }
      tx.success()
      tx.close()
    }


    val end = System.currentTimeMillis()
    val elapse = end - start
    println(elapse)
    println(s"create about ${n * 1000} nodes, total cost $elapse ms")
    println("each node costs : " + elapse / (1000 * n))

    db.shutdown()
  }
}
