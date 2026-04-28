package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.junit.jupiter.api.Nested
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

class SasAddressUpdatedEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class Successful {
    fun `updates address - publishes domain event`() {
      // create cluster with person with an address

      // stub sas callback

      publishSasEvent()

      // assert address updated
      // assert domain event published
    }
  }

  @Nested
  inner class FailureScenarios {
    fun `address not returned from sas - pushes event to dead letter queue`() {
    }

    fun `cpr address does not exist - pushed to dead letter queue`() {
    }

    fun `cpr person does not exist - pushes to dead letter queue`() {
    }
  }

  private fun publishSasEvent(crn: String, sasAddressId: String) {
    publishDomainEvent(
      SAS_ADDRESS_UPDATED,
      DomainEvent(
        eventType = SAS_ADDRESS_UPDATED,
        additionalInformation = AdditionalInformation(
          sourceCrn = "",
          sasAddressId = "",
        ),
      ),
    )
  }
}
