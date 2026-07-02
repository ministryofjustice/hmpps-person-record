package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED

data class ProbationOffenderCreated(
  override val eventType: String = NEW_OFFENDER_CREATED,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderUpdated(
  override val eventType: String,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

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

data class ProbationOffenderAddressCreated(
  override val eventType: String = OFFENDER_ADDRESS_CREATED,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressCreatedInfo,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressCreatedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String? = null,

  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationOffenderAddressUpdated(
  override val eventType: String = OFFENDER_ADDRESS_UPDATED,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressUpdatedInfo,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressUpdatedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationOffenderAddressDeleted(
  override val eventType: String = OFFENDER_ADDRESS_DELETED,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressDeletedInfo,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressDeletedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)
