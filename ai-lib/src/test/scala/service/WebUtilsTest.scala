package cn.panda.ailib.service

import org.junit.Test
import java.io.{File, FileInputStream}

import scala.collection.immutable.Map

class WebUtilsTest {
  @Test
  def test1(): Unit ={
    var image_path1 = "E:/[face]/unknown/test.jpg"
    var image_path2 = "E:/[face]/unknown/test2.jpg"
    var reqUrl = "http://127.0.0.1:8081/service/face/similarity/blob/"
    val file1 = new File(image_path1)
    val file2 = new File(image_path2)
    val in1 = new FileInputStream(file1)
    val in2 = new FileInputStream(file2)

    val contents:Map[String,FileInputStream] = Map("image1" -> in1, "image2" -> in2)

    val resStr = WebUtils.doPost(reqUrl,inStreamContents = contents)
    print(resStr)
  }

  @Test
  def test2(): Unit ={
    var image_path1 = "E:/[face]/unknown/test.jpg"
    var image_path2 = "E:/[face]/unknown/test2.jpg"
    var reqUrl = "http://127.0.0.1:8081/service/face/similarity/blob/"
    val file1 = new File(image_path1)
    val file2 = new File(image_path2)
    val in1 = new FileInputStream(file1)
    val in2 = new FileInputStream(file2)
    val contents:Map[String,String] = Map("image1" -> image_path1, "image2" -> image_path2)

    val resStr = WebUtils.doGet(reqUrl,contents)
    print(resStr)
  }

  @Test
  def test3(): Unit ={
    var image_path1 = "E:/[face]/unknown/test.jpg"
    var image_path2 = "E:/[face]/unknown/test2.jpg"
    var reqUrl = "http://127.0.0.1:8081/service/face/similarity/blob/"
    val file1 = new File(image_path1)
    val file2 = new File(image_path2)

    val contents:Map[String,File] = Map("image1" -> file1, "image2" -> file2)

    val resStr = WebUtils.doPost(reqUrl,fileContents=contents)
    print(resStr)
  }


}
