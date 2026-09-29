package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

const val PROBATION_PERSON_CREATED = "probation-case.engagement.created"
const val PROBATION_PERSON_UPDATED = "probation-case.personal-details.updated"
const val PROBATION_PERSON_DELETED = "probation-case.engagement.deleted"
const val PROBATION_PERSON_DELETED_GDPR = "probation-case.deleted.gdpr"
const val PROBATION_PERSON_MERGED = "probation-case.merge.completed"
const val PROBATION_PERSON_UNMERGED = "probation-case.unmerge.completed"
const val PROBATION_PERSON_RECOVERED = "probation-case.engagement.recovered"
const val PROBATION_ALIAS_CREATED = "probation-case.alias.created"
const val PROBATION_ALIAS_UPDATED = "probation-case.alias.updated"
const val PROBATION_ALIAS_DELETED = "probation-case.alias.deleted"
const val PROBATION_REFERENCE_CREATED = "probation-case.additional-identifier.created"
const val PROBATION_REFERENCE_DELETED = "probation-case.additional-identifier.deleted"
const val PROBATION_REFERENCE_RECOVERED = "probation-case.additional-identifier.recovered"
const val PROBATION_ADDRESS_CREATED = "probation-case.address.created"
const val PROBATION_ADDRESS_UPDATED = "probation-case.address.updated"
const val PROBATION_ADDRESS_DELETED = "probation-case.address.deleted"

data class ProbationPersonCreated(
  override val eventType: String = PROBATION_PERSON_CREATED,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.getCrn()
}

data class ProbationPersonUpdated(
  override val eventType: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.getCrn()
}

data class ProbationPersonDeleted(
  override val eventType: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.getCrn()
}

data class ProbationPersonRecovered(
  override val eventType: String = PROBATION_PERSON_RECOVERED,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.getCrn()
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
  val crn: String get() = personReference.getCrn()
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
  val crn: String get() = personReference.getCrn()
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
  val crn: String get() = personReference.getCrn()
}

data class ProbationAddressDeletedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

private fun PersonReference.getCrn() = this.identifiers?.first { it.type == "CRN" }?.value!!
