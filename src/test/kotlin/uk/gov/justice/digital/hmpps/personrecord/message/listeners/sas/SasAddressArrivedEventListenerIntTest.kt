package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate
import java.util.UUID

class SasAddressArrivedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `no existing main address exists - promotes address to main`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails().copy(
          addresses = listOf(Address(postcode = randomPostcode(), statusCode = AddressStatusCode.P)),
        ),
      )
      val originalAddressEntity = personEntity.addresses.first()
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalAddressEntity.updateId).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(AddressStatusCode.M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

//      publishSasAddressEvent(personEntity.crn!!, SAS_ADDRESS_ARRIVED)

      awaitAssert {
        val actualAddressEntity = addressRepository.findByUpdateId(originalAddressEntity.updateId!!)!!
        assertThat(actualAddressEntity.isVerified).isEqualTo(true)
        assertThat(actualAddressEntity.statusCode).isEqualTo(AddressStatusCode.M)
        assertThat(actualAddressEntity.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    }

    @Test
    fun `existing main address exists - demotes existing main address - promotes address to main`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails().copy(
          addresses = listOf(
            Address(postcode = randomPostcode(), statusCode = AddressStatusCode.M),
            Address(postcode = randomPostcode(), statusCode = AddressStatusCode.P),
          ),
        ),
      )
      val originalMainAddress = personEntity.addresses.first { it.statusCode == AddressStatusCode.M }
      val originalPreviousAddress = personEntity.addresses.first { it.statusCode == AddressStatusCode.P }
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalPreviousAddress.updateId).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(AddressStatusCode.M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

//      publishSasAddressEvent(personEntity.crn!!, SAS_ADDRESS_ARRIVED)

      awaitAssert {
        val actualDemotedAddress = addressRepository.findByUpdateId(originalMainAddress.updateId!!)!!
        assertThat(actualDemotedAddress.isVerified).isEqualTo(originalMainAddress.isVerified)
        assertThat(actualDemotedAddress.statusCode).isEqualTo(AddressStatusCode.P)
        assertThat(actualDemotedAddress.endDate).isEqualTo(LocalDate.now().toUkZonedDateTime())

        val actualPromotedAddress = addressRepository.findByUpdateId(originalPreviousAddress.updateId!!)!!
        assertThat(actualPromotedAddress.isVerified).isEqualTo(true)
        assertThat(actualPromotedAddress.statusCode).isEqualTo(AddressStatusCode.M)
        assertThat(actualPromotedAddress.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      expectMessagesOn(testOnlyCPRDomainEventsQueue, 2)
    }

    @Test
    fun `arrival update is for existing main address - keeps address as main`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails()
          .copy(addresses = listOf(Address(postcode = randomPostcode(), statusCode = AddressStatusCode.M))),
      )
      val originalMainAddress = personEntity.addresses.first()
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalMainAddress.updateId).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(AddressStatusCode.M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

//      publishSasAddressEvent(personEntity.crn!!, SAS_ADDRESS_ARRIVED)

      awaitAssert {
        val actualAddress = addressRepository.findByUpdateId(originalMainAddress.updateId!!)!!
        assertThat(actualAddress.isVerified).isEqualTo(true)
        assertThat(actualAddress.statusCode).isEqualTo(AddressStatusCode.M)
        assertThat(actualAddress.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    }
  }

  @Nested
  inner class FailureScenarios {

    @Test
    fun `address not returned from sas - pushes event to dead letter queue`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails()
          .copy(addresses = listOf(Address(postcode = randomPostcode(), statusCode = AddressStatusCode.P))),
      )
      createPersonKey().addPerson(personEntity)

      stubGetRequestToSas(null, status = 404)
//      publishSasAddressEvent(personEntity.crn!!, SAS_ADDRESS_ARRIVED)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
      val actualAddress = personRepository.findByCrn(personEntity.crn!!)!!.addresses.first()
      assertThat(actualAddress.statusCode).isEqualTo(AddressStatusCode.P)
    }

    @Test
    fun `cpr address does not exist - pushed to dead letter queue`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails()
          .copy(addresses = listOf(Address(postcode = randomPostcode(), statusCode = AddressStatusCode.P))),
      )
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, UUID.randomUUID()).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(AddressStatusCode.M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
//      publishSasAddressEvent(personEntity.crn!!, SAS_ADDRESS_ARRIVED)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
      val actualAddress = personRepository.findByCrn(personEntity.crn!!)!!.addresses.first()
      assertThat(actualAddress.statusCode).isEqualTo(AddressStatusCode.P)
    }
  }
}
