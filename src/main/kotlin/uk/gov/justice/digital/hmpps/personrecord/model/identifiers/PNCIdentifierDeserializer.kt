package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class PNCIdentifierDeserializer : ValueDeserializer<PNCIdentifier>() {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): PNCIdentifier = PNCIdentifier.from(parser?.text ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): PNCIdentifier = PNCIdentifier.from()
}
