import java.io.File

import cn.pandadb.semoplib.image.FaceSimilarityComparator
import org.junit.{Assert, Test}

/**
  * @Author: Airzihao
  * @Description:
  * @Date: Created at 18:26 2020/9/10
  * @Modified By:
  */
class FaceCacheTest {

  @Test
  def test1(): Unit = {
    val list1 = List(0.0,0.0)
    val list2 = List(3.0,4.0)
    val list3 = List(6.0,8.0)
    val list4 = List(1.0,math.sqrt(3))
    val list5 = List(1.0,0.0)
    val list6 = List(0.0,1.0)

    val comparator = new FaceSimilarityComparator
    Assert.assertEquals(0, comparator._featureSimilarity(list1, list2), 0.001)
    Assert.assertEquals(1, comparator._featureSimilarity(list2, list3), 0.001)
    Assert.assertEquals(0.5, comparator._featureSimilarity(list4, list5), 0.001)

    val l1 = List(list1, list2)
    val l2 = List(list3, list4, list5)
    val arr = l1.map(f1 => l2.map(f2 => comparator._featureSimilarity(f1, f2)).toArray).toArray.asInstanceOf[Array[Array[Double]]]
    println(arr)
  }

//  @Test
//  def test2(): Unit = {
//    val arr = _getFiles1(new File("./testinput/images"))
//    println(arr)
//  }

}
