package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class PNCIdentifierDeserializer : StdDeserializer<PNCIdentifier>(PNCIdentifier::class.java) {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): PNCIdentifier = PNCIdentifier.from(parser?.string ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): PNCIdentifier = PNCIdentifier.from()
}
