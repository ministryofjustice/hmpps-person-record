package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifiers(
  val defendantId: String? = null,
  val crn: String? = null,
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pnc: PNCIdentifier? = PNCIdentifier.from(),
  @JsonDeserialize(using = CROIdentifierDeserializer::class)
  val cro: CROIdentifier? = CROIdentifier.from(),
  @JsonProperty("prisonerNumber")
  val prisonNumber: String? = null,
  @JsonProperty("ni")
  val nationalInsuranceNumber: String? = null,
)
