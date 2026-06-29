package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_ARRIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

data class SasAddressUpdated(
  override val eventType: String = SAS_ADDRESS_UPDATED,
  val detailUrl: String,
) : HmppsDomainEvent

data class SasAddressDeleted(
  override val eventType: String = SAS_ADDRESS_DELETED,
  val additionalInformation: SasAddressDeletedInfo,
) : HmppsDomainEvent

data class SasAddressArrived(
  override val eventType: String = SAS_ADDRESS_ARRIVED,
  val detailUrl: String,
  val additionalInformation: SasAddressArrivedInfo,
) : HmppsDomainEvent

data class SasAddressArrivedInfo(
  val corePersonAddressId: String,
)

data class SasAddressDeletedInfo(
  @JsonProperty("corePersonAddressId")
  val cprAddressId: String,
)
