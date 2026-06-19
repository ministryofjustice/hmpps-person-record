package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "eventType",
  visible = true,
)
@JsonSubTypes(
  JsonSubTypes.Type(value = ProbationOffenderCreated::class, name = NEW_OFFENDER_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderUpdated::class, name = OFFENDER_PERSONAL_DETAILS_UPDATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressCreated::class, name = OFFENDER_ADDRESS_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressUpdated::class, name = OFFENDER_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressDeleted::class, name = OFFENDER_ADDRESS_DELETED),
  JsonSubTypes.Type(value = ProbationOffenderMerged::class, name = OFFENDER_MERGED),
  JsonSubTypes.Type(value = ProbationOffenderUnMerged::class, name = OFFENDER_UNMERGED),
  JsonSubTypes.Type(value = ProbationOffenderDeleted::class, names = [OFFENDER_DELETION, OFFENDER_GDPR_DELETION]),
  JsonSubTypes.Type(value = PrisonPrisonerCreated::class, name = PRISONER_CREATED),
  JsonSubTypes.Type(value = PrisonPrisonerUpdated::class, name = PRISONER_UPDATED),
  JsonSubTypes.Type(value = PrisonPrisonerMerged::class, name = PRISONER_MERGED),
  JsonSubTypes.Type(value = SasAddressUpdated::class, name = SAS_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = CprPersonCreated::class, names = [CPR_PRISON_PERSON_CREATED, CPR_PROBATION_PERSON_CREATED, CPR_COURT_PERSON_CREATED]),
  JsonSubTypes.Type(value = CprAddressCreated::class, name = CPR_PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = CprAddressUpdated::class, name = CPR_PROBATION_ADDRESS_UPDATED),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface DomainEvent {
  val eventType: String
  val version: Int
  val occurredAt: String
}

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

data class PrisonPrisonerCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMerged(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: PrisonPrisonerMergedInfo,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMergedInfo(
  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String,
)

data class SasAddressUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val detailUrl: String,
) : DomainEvent

data class CprPersonCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprAddressCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  override val description: String,
  override val detailUrl: String,
  override val personReference: PersonReference,
  override val additionalInformation: CprAddressCreatedInfo,
) : CprAddressDomainEvent

data class CprAddressCreatedInfo(
  override val cprAddressId: String,
  val deliusAddressId: Long? = null,
) : CprAddressInfo {
  override val deliusAddressIdAsString: String? get() = deliusAddressId?.toString()
}

data class CprAddressUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  override val description: String,
  override val detailUrl: String,
  override val personReference: PersonReference,
  override val additionalInformation: CprAddressUpdatedInfo,
) : CprAddressDomainEvent

data class CprAddressUpdatedInfo(
  override val cprAddressId: String,
  val deliusAddressId: String? = null,
) : CprAddressInfo {
  override val deliusAddressIdAsString: String? get() = deliusAddressId
}

sealed interface CprAddressDomainEvent : DomainEvent {
  val description: String
  val detailUrl: String
  val personReference: PersonReference
  val additionalInformation: CprAddressInfo
}

sealed interface CprAddressInfo {
  val cprAddressId: String
  val deliusAddressIdAsString: String?
}
