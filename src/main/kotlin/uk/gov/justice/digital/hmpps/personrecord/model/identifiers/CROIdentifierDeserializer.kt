package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class CROIdentifierDeserializer : ValueDeserializer<CROIdentifier>() {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CROIdentifier = CROIdentifier.from(parser?.text ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): CROIdentifier = CROIdentifier.from()
}
