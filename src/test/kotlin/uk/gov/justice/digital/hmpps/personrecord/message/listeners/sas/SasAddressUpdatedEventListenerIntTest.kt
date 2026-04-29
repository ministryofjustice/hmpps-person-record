package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class SasAddressUpdatedEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `updates address - publishes domain event`() {
      val personEntity = createPerson(createRandomProbationPersonDetails())
      createPersonKey()
        .addPerson(personEntity)

      val sasAddressId = randomDigit() // this will be an uuid
      stubGetRequestToSas(sasAddressId)

      publishSasEvent(personEntity.crn!!, sasAddressId)

      awaitAssert {
        assertThat(personRepository.findByCrn(personEntity.crn!!)!!.addresses.size).isEqualTo(2)
      }

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
          sourceCrn = crn,
          sasAddressId = sasAddressId,
        ),
      ),
    )
  }

  private fun stubGetRequestToSas(sasAddressId: String) {
    stubGetRequest(
      url = "/proposed-accommodations/$sasAddressId",
      body = """{ "postcode": "er3 123", "addressId": $sasAddressId }""".trimIndent(),
      status = 200,
    )
  }
}
