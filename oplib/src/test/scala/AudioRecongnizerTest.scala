package cn.panda.semop.test

import java.io.File

import cn.pandadb.oplib.audio.AudioRecongnizer
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob


class AudioRecongnizerTest extends TestBase {
  val audioRecongnizer = new AudioRecongnizer()
  audioRecongnizer.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "C:\\Users\\hai\\Desktop\\temp.wav"
    val res = audioRecongnizer.extract(Blob.fromFile(new File(imagePath1)))
    print(res)
  }
}




