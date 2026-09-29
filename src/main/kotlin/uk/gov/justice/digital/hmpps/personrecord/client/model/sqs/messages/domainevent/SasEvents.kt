package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

const val SAS_ADDRESS_UPDATED = "sas.accommodation.updated"
const val SAS_ADDRESS_ARRIVED = "sas.accommodation.person.arrived"
const val SAS_ADDRESS_DELETED = "sas.accommodation.deleted"

data class SasAddressUpdated(
  override val eventType: String = SAS_ADDRESS_UPDATED,
  val detailUrl: String,
) : DomainEvent

data class SasAddressArrived(
  override val eventType: String = SAS_ADDRESS_ARRIVED,
  val detailUrl: String,
  val additionalInformation: SasAddressArrivedInfo,
) : DomainEvent

data class SasAddressArrivedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String,
)

data class SasAddressDeleted(
  override val eventType: String = SAS_ADDRESS_DELETED,
  val additionalInformation: SasAddressDeletedInfo,
) : DomainEvent

data class SasAddressDeletedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String,
)
