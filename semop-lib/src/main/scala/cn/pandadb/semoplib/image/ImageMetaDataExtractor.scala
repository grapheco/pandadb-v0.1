package cn.pandadb.semoplib.image

import cn.pandadb.semop.{DomainType, SemanticExtractor, SubPropertyExtractor}
import javax.imageio.ImageIO
import org.neo4j.blob.Blob
import org.neo4j.blob.util.Configuration

/**
 * Created by bluejoe on 2019/2/17.
 */
@SemanticExtractor(name = "image", domain = DomainType.BlobImage)
class ImageMetaDataExtractor extends SubPropertyExtractor {
  override def declareProperties() = Map("width" -> classOf[Int], "height" -> classOf[String])

  override def extract(x: Any): Map[String, Any] = x.asInstanceOf[Blob].offerStream((is) => {
    val srcImage = ImageIO.read(is);
    Map("height" -> srcImage.getHeight(), "width" -> srcImage.getWidth());
  })

  override def initialize(conf: Configuration): Unit = {

  }
}