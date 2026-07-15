package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_RECOVERED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_UNMERGED

data class ProbationPersonCreated(
  override val eventType: String = PROBATION_PERSON_CREATED,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationPersonUpdated(
  override val eventType: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationPersonDeleted(
  override val eventType: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationPersonRecovered(
  override val eventType: String = PROBATION_PERSON_RECOVERED,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationPersonMerged(
  override val eventType: String = PROBATION_PERSON_MERGED,
  val additionalInformation: ProbationPersonMergedInfo,
) : DomainEvent

data class ProbationPersonMergedInfo(
  @JsonProperty("sourceCRN")
  val sourceCrn: String,

  @JsonProperty("targetCRN")
  val targetCrn: String,
)

data class ProbationPersonUnmerged(
  override val eventType: String = PROBATION_PERSON_UNMERGED,
  val additionalInformation: ProbationPersonUnmergedInfo,
) : DomainEvent

data class ProbationPersonUnmergedInfo(
  @JsonProperty("reactivatedCRN")
  val reactivatedCrn: String,

  @JsonProperty("unmergedCRN")
  val unmergedCrn: String,
)

data class ProbationAddressCreated(
  override val eventType: String = PROBATION_ADDRESS_CREATED,
  val personReference: PersonReference,
  val additionalInformation: ProbationAddressCreatedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationAddressCreatedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String? = null,

  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationAddressUpdated(
  override val eventType: String = PROBATION_ADDRESS_UPDATED,
  val personReference: PersonReference,
  val additionalInformation: ProbationAddressUpdatedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationAddressUpdatedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationAddressDeleted(
  override val eventType: String = PROBATION_ADDRESS_DELETED,
  val personReference: PersonReference,
  val additionalInformation: ProbationAddressDeletedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationAddressDeletedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)
