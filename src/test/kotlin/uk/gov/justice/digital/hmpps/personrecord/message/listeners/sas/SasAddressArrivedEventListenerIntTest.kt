package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_ARRIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class SasAddressArrivedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `no existing main address exists - promotes address to main`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), statusCode = AddressStatusCode.P))))
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey().addPerson(existingPersonEntity)
      val crn = existingPersonEntity.crn

      val sasCallbackResponse = createSasAddressGetResponse(crn, existingAddressEntity.updateId).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(AddressStatusCode.M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      publishSasAddressEvent(crn!!, SAS_ADDRESS_ARRIVED)

      awaitAssert {
        val addressEntity = addressRepository.findByUpdateId(existingAddressEntity.updateId!!)!!
        assertThat(addressEntity.isVerified).isEqualTo(true)
        assertThat(addressEntity.statusCode).isEqualTo(AddressStatusCode.M)
        assertThat(addressEntity.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      assertDomainEventPublishedAfterSasEvent(
        expectedEventType = CPR_PROBATION_ADDRESS_UPDATED,
        crn = crn,
      )
    }

    @Test
    fun `existing main address exists - demotes existing main address - promotes address to main`() {
    }
  }

  @Nested
  inner class FailureScenarios {

    @Test
    fun `address not returned from sas - pushes event to dead letter queue`() {
    }

    @Test
    fun `cpr address does not exist - pushed to dead letter queue`() {
    }
  }
}
