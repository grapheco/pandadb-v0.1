package cn.pandadb.blobstorage.buffer.exception

class ConfigException (private val message: String = "",
                       private val cause: Throwable = None.orNull
                      ) extends RuntimeException(message, cause) {
}

class FilePathIsDirectoryException (private val message: String = "",
                       private val cause: Throwable = None.orNull
                      ) extends ConfigException(message, cause) {
}

class FilePathIsNotDirectoryException (private val message: String = "",
                                    private val cause: Throwable = None.orNull
                                   ) extends ConfigException(message, cause) {
}