package cn.panda.semop.test

import cn.pandadb.oplib.text.SentimentClassifier
import org.junit.{Assert, Test}


class SentimentClassifierTest extends TestBase {

  val sentimentExtractor = new SentimentClassifier()
  sentimentExtractor.initialize(config)

  @Test
  def test1():Unit={
    var text = "真正懂得微笑的人，总是容易获得比别人更多的机会，总是容易取得成功，总是能获取更多的更多。舍得微笑，得到的是友谊；舍得微笑，拥抱的是快乐；舍得微笑，获取的是幸福。岁月峥嵘，几度春秋，人生几何？然修行微笑，自爱于心，心暖花开！"
    val res = sentimentExtractor.extract(text)
    print(res)
  }
}



