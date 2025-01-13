package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.CurrentlyManaged

class CurrentlyManagedDeserializer : StdDeserializer<Boolean>(Boolean::class.java) {
  override fun deserialize(parser: JsonParser?, context: DeserializationContext?): Boolean? = CurrentlyManaged.from(parser?.text ?: "").status

  override fun getNullValue(ctxt: DeserializationContext?): Boolean? = null
}
