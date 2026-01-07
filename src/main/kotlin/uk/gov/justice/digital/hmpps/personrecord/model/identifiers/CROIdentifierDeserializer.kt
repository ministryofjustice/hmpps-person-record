package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class CROIdentifierDeserializer : StdDeserializer<CROIdentifier>(CROIdentifier::class.java) {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CROIdentifier = CROIdentifier.from(parser?.string ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): CROIdentifier = CROIdentifier.from()
}
