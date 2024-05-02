package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifiers(
  val crn: String,
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pnc: PNCIdentifier? = PNCIdentifier.from(),
)
