package cn.panda.semop.test
import java.io.File

import cn.pandadb.semoplib.image.FaceSimilarityComparator
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory


class FaceSimilarityComparatorTest extends TestBase {
  val simComparator = new FaceSimilarityComparator()
  simComparator.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "testinput/photo1.jpg"
    var imagePath2 = "testinput/photo2.jpg"
    val res = simComparator.compareAsSets(BlobFactory.fromFile(new File(imagePath1)),
      BlobFactory.fromFile(new File(imagePath2)))
    print(res)
  }
}
