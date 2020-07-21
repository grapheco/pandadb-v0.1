package cn.pandadb.blobstorage.buffer.exception

class BufferException (private val message: String = "",
                            private val cause: Throwable = None.orNull
                           ) extends RuntimeException(message, cause) {
}
class NotBindingException (private val message: String = "",
                            private val cause: Throwable = None.orNull
                           ) extends BufferException(message, cause) {
}