package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

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
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprAddressCreatedInfo,
) : DomainEvent

data class CprAddressCreatedInfo(
  val cprAddressId: String,
  val deliusAddressId: Long? = null,
)

data class CprAddressUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val description: String,
  val detailUrl: String,
  val personReference: PersonReference,
  val additionalInformation: CprAddressUpdatedInfo,
) : DomainEvent

data class CprAddressUpdatedInfo(
  val cprAddressId: String,
  val deliusAddressId: Long? = null,
)
