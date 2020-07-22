package org.neo4j.cypher.internal.util.v3_4.symbols

object BlobType {
  val instance = new BlobType() {
    override val parentType = CTAny
    override val toString = "Blob"
    override val toNeoTypeString = "BLOB?"
  }
}

sealed trait BlobType extends CypherType