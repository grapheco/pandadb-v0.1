package cn.panda.semop.test
import java.io.File

import cn.pandadb.oplib.image.FaceSimilarityComparator
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob


class FaceSimilarityComparatorTest extends TestBase {
  val simComparator = new FaceSimilarityComparator()
  simComparator.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "testinput/photo1.jpg"
    var imagePath2 = "testinput/photo2.jpg"
    val res = simComparator.compareAsSets(Blob.fromFile(new File(imagePath1)),Blob.fromFile(new File(imagePath2)))
    print(res)
  }



}
