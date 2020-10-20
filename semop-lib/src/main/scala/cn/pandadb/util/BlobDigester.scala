package cn.pandadb.util

import org.apache.commons.codec.digest.DigestUtils
import org.neo4j.blob.Blob

/**
 * @Author: Airzihao
 * @Description:
 * @Date: Created at 17:15 2020/10/20
 * @Modified By:
 */

object BlobDigester {
  def getMd5HexDigest(blob: Blob): String = {
    DigestUtils.md5Hex(blob.toBytes())
  }
}