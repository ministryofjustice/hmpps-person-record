package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class ProbationOffenderDeleted(
  override val eventType: String,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderMerged(
  override val eventType: String,
  val additionalInformation: ProbationOffenderMergedInfo,
) : HmppsDomainEvent

data class ProbationOffenderMergedInfo(
  @JsonProperty("sourceCRN")
  val sourceCrn: String,

  @JsonProperty("targetCRN")
  val targetCrn: String,
)

data class ProbationOffenderUnMerged(
  override val eventType: String,
  val additionalInformation: ProbationOffenderUnMergedInfo,
) : HmppsDomainEvent

data class ProbationOffenderUnMergedInfo(
  @JsonProperty("reactivatedCRN")
  val reactivatedCrn: String,

  @JsonProperty("unmergedCRN")
  val unmergedCrn: String,
)
