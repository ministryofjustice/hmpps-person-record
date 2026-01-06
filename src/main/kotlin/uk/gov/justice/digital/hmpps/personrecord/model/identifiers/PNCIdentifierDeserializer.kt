package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class PNCIdentifierDeserializer : StdDeserializer<PNCIdentifier>(PNCIdentifier::class.java) {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): PNCIdentifier = PNCIdentifier.from(parser?.text ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): PNCIdentifier = PNCIdentifier.from()
}
