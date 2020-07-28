package cn.panda.semop.test
import java.io.File

import cn.pandadb.oplib.image.PlateNumberExtractor
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob


class PlateNumberExtractorTest extends TestBase {
  val plateExtractor = new PlateNumberExtractor()
  plateExtractor.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "testinput/plate_number1.jpg"
    val res = plateExtractor.extract(Blob.fromFile(new File(imagePath1)))
    assert(res.get("plateNumber").isDefined && res.get("plateNumber").get.equals("苏E730V7"))
    print(res)
  }
}
