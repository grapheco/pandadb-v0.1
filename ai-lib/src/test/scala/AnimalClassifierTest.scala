package cn.panda.ailib.test

import java.io.File
import org.junit.{Assert, Test}

import cn.panda.ailib.image.DogOrCatClassifier
import cn.pandadb.commons.blob.Blob


class AnimalClassifierTest extends TestBase {
  val plateExtractor = new DogOrCatClassifier()
  plateExtractor.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "C:\\Users\\hai\\Desktop\\cat.1.jpg"
    val res = plateExtractor.extract(Blob.fromFile(new File(imagePath1)))
    print(res)
  }

}
