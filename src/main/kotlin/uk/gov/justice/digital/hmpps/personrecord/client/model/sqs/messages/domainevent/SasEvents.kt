package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class SasAddressUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val detailUrl: String,
) : DomainEvent

data class SasAddressDeleted(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val additionalInformation: SasAddressDeletedInfo,
) : DomainEvent

data class SasAddressDeletedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String? = null,
)
