package org.neo4j.cypher.internal.frontend.v3_4.parser

import java.io.File

import cn.pandadb.commons.blob.Blob
import cn.pandadb.database.BlobPropertyStoreService
import org.apache.commons.codec.binary.Base64
import org.neo4j.cypher.internal.util.v3_4.InputPosition
import org.neo4j.cypher.internal.util.v3_4.symbols._
import org.neo4j.cypher.internal.v3_4.expressions._

trait BlobURL {
  def asCanonicalString: String;

  def createBlob(bpss: BlobPropertyStoreService): Blob;
}

case class BlobLiteralExpr(value: BlobURL)(val position: InputPosition) extends Expression {
  override def asCanonicalStringVal = value.asCanonicalString
}

case class BlobFileURL(filePath: String) extends BlobURL {
  override def asCanonicalString = filePath

  def createBlob(bpss: BlobPropertyStoreService): Blob = Blob.fromFile(new File(filePath))
}

case class BlobBase64URL(base64: String) extends BlobURL {
  override def asCanonicalString = base64

  def createBlob(bpss: BlobPropertyStoreService): Blob = Blob.fromBytes(Base64.decodeBase64(base64))
}

case class InternalUrl(blobId: String) extends BlobURL {
  override def asCanonicalString = blobId

  //TODO: use bolt session
  def createBlob(bpss: BlobPropertyStoreService): Blob =
    bpss.blobStorage.loadBatch(Array(bpss.blobIdFactory.fromLiteralString(blobId))).head.get;
}

case class BlobHttpURL(url: String) extends BlobURL {
  override def asCanonicalString = url

  def createBlob(bpss: BlobPropertyStoreService): Blob = Blob.fromHttpURL(url)
}

case class BlobFtpURL(url: String) extends BlobURL {
  override def asCanonicalString = url

  def createBlob(bpss: BlobPropertyStoreService): Blob = Blob.fromURL(url)
}

case class AlgoNameWithThresholdExpr(algorithm: Option[String], threshold: Option[Double])(val position: InputPosition)
  extends Expression {
}

case class CustomPropertyExpr(map: Expression, propertyKey: PropertyKeyName)(val position: InputPosition) extends LogicalProperty {
  override def asCanonicalStringVal = s"${map.asCanonicalStringVal}.${propertyKey.asCanonicalStringVal}"
}

case class SemanticLikeExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticUnlikeExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticCompareExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTFloat)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticSetCompareExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTList(CTList(CTFloat)))
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticContainExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticInExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticContainSetExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}

case class SemanticSetInExpr(lhs: Expression, ant: Option[AlgoNameWithThresholdExpr], rhs: Expression)(val position: InputPosition)
  extends Expression with BinaryOperatorExpression {
  override val signatures = Vector(
    TypeSignature(argumentTypes = Vector(CTAny, CTAny), outputType = CTBoolean)
  )

  override def canonicalOperatorSymbol = this.getClass.getSimpleName
}
