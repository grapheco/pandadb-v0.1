import java.io.File

/**
  * @Author: Airzihao
  * @Description:
  * @Date: Created at 15:16 2020/9/7
  * @Modified By:
  */
object FilePathUtils {

  val osName = System.getProperty("os.name").toLowerCase

  private def isWindows(): Boolean = {
    if (osName.indexOf("windows")> -1) true else false
  }
  private def isLinux(): Boolean = {
    if (osName.indexOf("linux")> -1) true else false
  }
  private def isMac(): Boolean = {
    if (osName.indexOf("mac")> -1) true else false
  }
  private def isOthers(): Boolean = {
    if(isWindows() | isLinux() | isMac()) false else true
  }

  def transSplitSymbol(path: String): String = {
    if (isWindows()) {
      return path.replace("\\","/")

    }
    return path
  }
}
