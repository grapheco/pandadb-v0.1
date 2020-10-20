import java.io.{File, FileInputStream}
import java.nio.file.{Files, Paths}

import cn.pandadb.util.BlobDigester
import org.apache.commons.codec.digest.DigestUtils
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory
/**
 * @Author: Airzihao
 * @Description:
 * @Date: Created at 10:16 2020/10/20
 * @Modified By:
 */

class BlobTest {
  val imageFilePath = "./src/test/resources/10kB"
  val imageFile: File = new File(imageFilePath)
  val imageFileInputStream: FileInputStream = new FileInputStream(imageFile)
  val imageBytesArr: Array[Byte] = Files.readAllBytes(Paths.get(imageFilePath))
  val blob: Blob = BlobFactory.fromFile(imageFile)

  @Test
  def digestTest(): Unit = {
    val blobDigest: String = BlobDigester.getMd5HexDigest(blob)
    val isArrDigest: String = DigestUtils.md5Hex(imageFileInputStream)
    val bytesArrDigest: String = DigestUtils.md5Hex(imageBytesArr)
    Assert.assertEquals(blobDigest, isArrDigest)
    Assert.assertEquals(isArrDigest, bytesArrDigest)
  }

  @Test
  def speedTest(): Unit = {
    val counts = 10000
    println(s"blob size ${blob.length/1024}kB")
    println(s"conunts: $counts")

    val time0 = System.currentTimeMillis()
    for(i<-1 to counts) {
      val blobDigest = BlobDigester.getMd5HexDigest(blob)
    }
    val time1 = System.currentTimeMillis()
    val delta = time1 - time0
    println(s"cost time: $delta ms")
    println(s"${delta/counts}ms per blob.")
  }
}