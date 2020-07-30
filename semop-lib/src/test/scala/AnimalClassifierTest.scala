package cn.panda.semop.test

import java.io.File

import cn.pandadb.semoplib.image.DogOrCatClassifier
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory


class AnimalClassifierTest extends TestBase {
  val plateExtractor = new DogOrCatClassifier()
  plateExtractor.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "testinput/cat1.jpg"
    val res = plateExtractor.extract(BlobFactory.fromFile(new File(imagePath1)))
    print(res)
  }

}
