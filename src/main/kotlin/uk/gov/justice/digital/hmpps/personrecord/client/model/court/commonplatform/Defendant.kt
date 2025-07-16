package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.annotation.Nullable
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer

@JsonIgnoreProperties(ignoreUnknown = true)
data class Defendant(
  val id: String? = null,
  val masterDefendantId: String? = null,
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  var pncId: PNCIdentifier? = PNCIdentifier.from(),
  @JsonProperty("croNumber")
  @JsonDeserialize(using = CROIdentifierDeserializer::class)
  var cro: CROIdentifier? = CROIdentifier.from(),
  @Valid
  @Nullable
  val personDefendant: PersonDefendant? = null,
  val ethnicity: Ethnicity? = null,
  val aliases: List<DefendantAlias>? = emptyList(),
  val isYouth: Boolean = false,
  val isPncMissing: Boolean = false,
  val isCroMissing: Boolean = false,
)
