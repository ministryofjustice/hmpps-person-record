package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier

class CROIdentifierDeserializer : StdDeserializer<CROIdentifier>(CROIdentifier::class.java) {

  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): CROIdentifier =
    CROIdentifier.from(parser?.text ?: "")

  override fun getNullValue(ctxt: DeserializationContext?): CROIdentifier =
    CROIdentifier.from()
}
