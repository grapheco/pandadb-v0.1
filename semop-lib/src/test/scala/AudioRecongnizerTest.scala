package cn.panda.semop.test

import java.io.File

import cn.pandadb.semoplib.audio.AudioRecongnizer
import org.junit.{Assert, Test}
import org.neo4j.blob.Blob
import org.neo4j.blob.impl.BlobFactory


class AudioRecongnizerTest extends TestBase {
  val audioRecongnizer = new AudioRecongnizer()
  audioRecongnizer.initialize(config)

  @Test
  def test1():Unit={
    var imagePath1 = "./temp.wav"
    val res = audioRecongnizer.extract(BlobFactory.fromFile(new File(imagePath1)))
    print(res)
  }
}




