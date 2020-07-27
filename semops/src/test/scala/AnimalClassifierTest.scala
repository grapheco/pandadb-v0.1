package cn.panda.semop.test

import java.io.File

import org.junit.{Assert, Test}
import cn.pandadb.commons.blob.Blob
import cn.pandadb.semop.image.DogOrCatClassifier


class AnimalClassifierTest extends TestBase {
  val plateExtractor = new DogOrCatClassifier()
  plateExtractor.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "testinput/cat1.jpg"
    val res = plateExtractor.extract(Blob.fromFile(new File(imagePath1)))
    print(res)
  }

}
