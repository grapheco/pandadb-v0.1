package cn.panda.semop.service

import java.io.{File, FileInputStream}

import cn.panda.semop.test.TestBase
import org.junit.{Assert, Test}


class ServicesTest extends TestBase with ServiceInitializer {
  val hostUrl = "http://10.0.86.128:8081/"
  initialize(config)
  @Test
  def test1(): Unit = {
    var image_path1 = "E:/[face]/zdy1.jpg"
    var image_path2 = "E:/[face]/gy2.jpg"
    val file1 = new File(image_path1)
    val file2 = new File(image_path2)
    val in1 = new FileInputStream(file1)
    val in2 = new FileInputStream(file2)
    val sim = service.computeFaceSimilarity(in1, in2)
    print(sim.head)
  }


  @Test
  def test2(): Unit = {
    var image_path1 = "E:\\[pidb-ai-code]\\plate_number\\test1.jpg"
    val file1 = new File(image_path1)
    val in1 = new FileInputStream(file1)
    val plate = service.extractPlateNumber(in1)
    print(plate)
  }

  @Test
  def test3(): Unit = {
    var image_path1 = "C:\\Users\\hai\\Desktop\\cat.1.jpg"
    val file1 = new File(image_path1)
    val in1 = new FileInputStream(file1)
    val animal = service.classifyAnimal(in1)
    print(animal)
  }

  @Test
  def test4(): Unit = {
    val audio_path1 = "C:\\Users\\hai\\Desktop\\temp.wav"
    val file1 = new File(audio_path1)
    val in1 = new FileInputStream(file1)
    val content = service.mandarinASR(in1)
    print(content)
  }

  @Test
  def testSentiment(): Unit = {
    var text = "非常不开心"

    val sentiment = service.sentimentClassifier(text)
    print(sentiment)
  }

  @Test
  def testSegmentText(): Unit = {
    var text = "真正懂得微笑的人，总是容易获得比别人更多的机会，总是容易取得成功，总是能获取更多的更多。舍得微笑，得到的是友谊；舍得微笑，拥抱的是快乐；舍得微笑，获取的是幸福。岁月峥嵘，几度春秋，人生几何？然修行微笑，自爱于心，心暖花开！"

    val words = service.segmentText(text)
    println(words.size)
    println(words)
  }


}
