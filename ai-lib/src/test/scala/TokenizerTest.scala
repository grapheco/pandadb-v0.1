package cn.panda.ailib.test
import org.junit.Test
import cn.panda.ailib.text.ChineseTokenizer

class TokenizerTest extends TestBase {

  val tokenizer = new ChineseTokenizer()
  tokenizer.initialize(config)

  @Test
  def test1():Unit={
    var text = "真正懂得微笑的人，总是容易获得比别人更多的机会，总是容易取得成功，总是能获取更多的更多。舍得微笑，得到的是友谊；舍得微笑，拥抱的是快乐；舍得微笑，获取的是幸福。岁月峥嵘，几度春秋，人生几何？然修行微笑，自爱于心，心暖花开！"
    val res = tokenizer.extract(text)
    println(res("words").length)
    res("words").foreach(w=>print(s"$w | "))
  }

  @Test
  def test2():Unit={
    var text = ""
    val res = tokenizer.extract(text)
    println(res("words").length)
    println(res("words"))
    res("words").foreach(w=>print(s"$w | "))
  }

  @Test
  def test3():Unit={
    var text = null
    val res = tokenizer.extract(text)
    println(res("words").length)
    println(res("words"))
    res("words").foreach(w=>print(s"$w | "))
  }
}
