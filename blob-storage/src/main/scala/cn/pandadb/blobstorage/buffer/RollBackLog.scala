package cn.pandadb.blobstorage.buffer

import java.io.{BufferedReader, File, FileReader, FileWriter}

import cn.pandadb.blobstorage.buffer.exception.FilePathIsDirectoryException

class RollBackLogReader (filePath : File) extends Iterator[String]{
  if (filePath.isDirectory) throw new FilePathIsDirectoryException(s"The file path ${filePath.getPath} is a directory")
  if (!filePath.exists()) filePath.createNewFile()
  final val reader = new BufferedReader(new FileReader(filePath))
  final var current : String = _
  final def hasNext : Boolean = {
    current = reader.readLine()
    current == null
  }
  final def next : String = {
    current
  }

  final def clean() : Unit = filePath.delete()

  def getWriter : RollBackLogWriter = {
    new RollBackLogWriter(filePath)
  }
}

class RollBackLogWriter (filePath : File) {
  if (filePath.isDirectory) throw new FilePathIsDirectoryException(s"The file path ${filePath.getPath} is a directory")
  if (!filePath.exists()) filePath.createNewFile()

  final val r = new FileWriter(filePath)

  final def append(str : String) : Unit = {
    r.append(str)
    flush()
  }
  final def append(str : Iterable[String]) : Unit = {
    for (s <- str) {
      r.append(s)
    }
    flush()
  }
  final def flush() : Unit = r.flush()

  final def close() : Unit = r.close()

  final def clean() : Unit = filePath.delete()

  def getReader : RollBackLogReader = {
    new RollBackLogReader(filePath)
  }
}
