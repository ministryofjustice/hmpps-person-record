package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import java.time.Instant
import java.util.UUID

data class CprPersonCreated(
  override val eventType: String,
  val version: Int = 1,
  val occurredAt: String,
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
