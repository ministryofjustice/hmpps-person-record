package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.deserializers.CROIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.deserializers.PNCIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier

@JsonIgnoreProperties(ignoreUnknown = true)
data class Defendant(
  val id: String? = null,
  val masterDefendantId: String? = null,
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pncId: PNCIdentifier? = PNCIdentifier.from(),
  @JsonProperty("croNumber")
  @JsonDeserialize(using = CROIdentifierDeserializer::class)
  val cro: CROIdentifier? = CROIdentifier.from(),
  @Valid
  val personDefendant: PersonDefendant? = null,
  val ethnicity: Ethnicity? = null,
  val aliases: List<DefendantAlias>? = emptyList(),

)
