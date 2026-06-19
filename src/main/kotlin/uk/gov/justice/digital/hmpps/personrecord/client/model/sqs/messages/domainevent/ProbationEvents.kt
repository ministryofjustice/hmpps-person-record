package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class ProbationOffenderCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressCreatedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressCreatedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String? = null,

  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationOffenderAddressUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressUpdatedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressUpdatedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String? = null,

  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationOffenderAddressDeleted(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: ProbationOffenderAddressDeletedInfo,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}

data class ProbationOffenderAddressDeletedInfo(
  @JsonProperty("addressId")
  val deliusAddressId: Long,
)

data class ProbationOffenderMerged(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val additionalInformation: ProbationOffenderMergedInfo,
) : DomainEvent

data class ProbationOffenderMergedInfo(
  @JsonProperty("sourceCRN")
  val sourceCrn: String,

  @JsonProperty("targetCRN")
  val targetCrn: String,
)

data class ProbationOffenderUnMerged(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val additionalInformation: ProbationOffenderUnMergedInfo,
) : DomainEvent

data class ProbationOffenderUnMergedInfo(
  @JsonProperty("reactivatedCRN")
  val reactivatedCrn: String,

  @JsonProperty("unmergedCRN")
  val unmergedCrn: String,
)

data class ProbationOffenderDeleted(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}
