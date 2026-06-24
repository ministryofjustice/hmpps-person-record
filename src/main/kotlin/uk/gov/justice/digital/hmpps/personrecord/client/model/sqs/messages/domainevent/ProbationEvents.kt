package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED

data class ProbationOffenderDeleted(
  override val eventType: String,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderMerged(
  override val eventType: String = OFFENDER_MERGED,
  val additionalInformation: ProbationOffenderMergedInfo,
) : HmppsDomainEvent

data class ProbationOffenderMergedInfo(
  @JsonProperty("sourceCRN")
  val sourceCrn: String,

  @JsonProperty("targetCRN")
  val targetCrn: String,
)

data class ProbationOffenderUnmerged(
  override val eventType: String = OFFENDER_UNMERGED,
  val additionalInformation: ProbationOffenderUnmergedInfo,
) : HmppsDomainEvent

data class ProbationOffenderUnmergedInfo(
  @JsonProperty("reactivatedCRN")
  val reactivatedCrn: String,

  @JsonProperty("unmergedCRN")
  val unmergedCrn: String,
)
