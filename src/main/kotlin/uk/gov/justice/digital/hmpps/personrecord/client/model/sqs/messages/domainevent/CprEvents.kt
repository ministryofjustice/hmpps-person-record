package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import java.time.Instant
import java.util.UUID

const val CPR_PRISON_PERSON_CREATED = "core-person-record.prison.record.created"
const val CPR_PRISON_PERSON_MERGED = "core-person-record.prison.record.merged"
const val CPR_PRISON_RELIGION_CREATED = "core-person-record.prison.religion.created"
const val CPR_PRISON_RELIGION_UPDATED = "core-person-record.prison.religion.updated"
const val CPR_PRISON_CONTACT_CREATED = "core-person-record.prison.contact.created"
const val CPR_PRISON_CONTACT_UPDATED = "core-person-record.prison.contact.updated"

const val CPR_PROBATION_PERSON_CREATED = "core-person-record.probation.record.created"
const val CPR_PROBATION_PERSON_DELETED = "core-person-record.probation.record.deleted"
const val CPR_PROBATION_PERSON_UPDATED = "core-person-record.probation.record.updated"
const val CPR_PROBATION_PERSON_MERGED = "core-person-record.probation.record.merged"
const val CPR_PROBATION_PERSON_UNMERGED = "core-person-record.probation.record.unmerged"

const val CPR_PROBATION_ADDRESS_CREATED = "core-person-record.probation.address.created"
const val CPR_PROBATION_ADDRESS_UPDATED = "core-person-record.probation.address.updated"
const val CPR_PROBATION_ADDRESS_DELETED = "core-person-record.probation.address.deleted"

data class CprPersonCreated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String,
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprPersonUpdated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String,
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprPersonDeleted(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String,
  val description: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprPersonMerged(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprPersonUnmerged(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
) : DomainEvent

data class CprAddressCreated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprAddressCreatedInfo,
) : DomainEvent

data class CprAddressCreatedInfo(
  val cprAddressId: UUID,
  val deliusAddressId: Long? = null,
)

data class CprAddressUpdated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprAddressUpdatedInfo,
) : DomainEvent

data class CprAddressUpdatedInfo(
  val cprAddressId: UUID,
  val deliusAddressId: Long? = null,
)

data class CprAddressDeleted(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val personReference: PersonReference,
  val additionalInformation: CprAddressDeletedInfo,
) : DomainEvent

data class CprAddressDeletedInfo(
  val cprAddressId: UUID,
  val deliusAddressId: Long? = null,
)

data class CprReligionCreated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val personReference: PersonReference,
  val additionalInformation: CprReligionCreatedInfo,
  val detailUrl: String = "",
) : DomainEvent

data class CprReligionCreatedInfo(
  val cprReligionId: UUID,
)

data class CprReligionUpdated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val additionalInformation: CprReligionUpdatedInfo,
  val personReference: PersonReference,
  val detailUrl: String = "",
) : DomainEvent

data class CprReligionUpdatedInfo(
  val cprReligionId: UUID,
)

data class CprContactCreated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprContactCreatedInfo,
) : DomainEvent

data class CprContactCreatedInfo(
  val cprContactId: UUID,
)

data class CprContactUpdated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String = Instant.now().asStringWithUkZone(),
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprContactUpdatedInfo,
) : DomainEvent

data class CprContactUpdatedInfo(
  val cprContactId: UUID,
)
