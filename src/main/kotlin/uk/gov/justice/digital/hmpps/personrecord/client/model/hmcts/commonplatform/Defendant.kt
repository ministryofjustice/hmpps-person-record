package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer

@JsonIgnoreProperties(ignoreUnknown = true)
data class Defendant(
  val id: String? = null,
  val masterDefendantId: String? = null,
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pncId: PNCIdentifier? = PNCIdentifier.from(),
  val croNumber: String? = null,
  @Valid
  val personDefendant: PersonDefendant? = null,
  val ethnicity: Ethnicity? = null,
  val aliases: List<DefendantAlias>? = emptyList(),

)
