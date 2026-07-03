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
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.P
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class SasAddressArrivedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `no existing main address exists - promotes address to main`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails(),
        configure = addAddressToRecord(
          Address(postcode = randomPostcode(), statusCode = P),
        ),
      )
      val originalAddressEntity = personEntity.addresses.first()
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalAddressEntity).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))

      publishSasAddressArrivedEvent(originalAddressEntity.updateId!!)

      awaitAssert {
        val actualAddressEntity = addressRepository.findByUpdateId(originalAddressEntity.updateId!!)!!
        assertThat(actualAddressEntity.isVerified).isEqualTo(true)
        assertThat(actualAddressEntity.statusCode).isEqualTo(M)
        assertThat(actualAddressEntity.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      assertCprAddressUpdatedEventPublished(personEntity.crn!!, originalAddressEntity.updateId!!, null, CPR)
    }

    @Test
    fun `existing main address exists - demotes existing main address - promotes address to main`() {
      val crn = randomCrn()
      createPersonKey().addPerson(
        createPerson(
          createRandomProbationPersonDetails(crn = crn),
        ).apply(
          addAddressToRecord(Address(postcode = randomPostcode(), statusCode = M)),
        )
          .apply(addAddressToRecord(Address(postcode = randomPostcode(), statusCode = P))),
      )

      val personEntity = personRepository.findByCrn(crn)!!
      val originalMainAddress = personEntity.addresses.first { it.statusCode == M }
      val originalPreviousAddress = personEntity.addresses.first { it.statusCode == P }

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalPreviousAddress).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      publishSasAddressArrivedEvent(originalPreviousAddress.updateId!!)

      awaitAssert {
        val actualDemotedAddress = addressRepository.findByUpdateId(originalMainAddress.updateId!!)!!
        assertThat(actualDemotedAddress.isVerified).isEqualTo(originalMainAddress.isVerified)
        assertThat(actualDemotedAddress.statusCode).isEqualTo(P)
        assertThat(actualDemotedAddress.endDate).isEqualTo(sasCallbackResponse.startDate!!.toUkZonedDateTime())

        val actualPromotedAddress = addressRepository.findByUpdateId(originalPreviousAddress.updateId!!)!!
        assertThat(actualPromotedAddress.isVerified).isEqualTo(true)
        assertThat(actualPromotedAddress.statusCode).isEqualTo(M)
        assertThat(actualPromotedAddress.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      assertCprAddressUpdatedEventPublished(personEntity.crn!!, originalMainAddress.updateId!!, null, CPR)
      assertCprAddressUpdatedEventPublished(personEntity.crn!!, originalPreviousAddress.updateId!!, null, CPR)
    }

    @Test
    fun `arrival update is for existing main address - keeps address as main`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails(),
        configure = addAddressToRecord(Address(postcode = randomPostcode(), statusCode = M)),
      )
      val originalMainAddress = personEntity.addresses.first()
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalMainAddress).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))

      publishSasAddressArrivedEvent(originalMainAddress.updateId!!)

      awaitAssert {
        val actualAddress = addressRepository.findByUpdateId(originalMainAddress.updateId!!)!!
        assertThat(actualAddress.isVerified).isEqualTo(true)
        assertThat(actualAddress.statusCode).isEqualTo(M)
        assertThat(actualAddress.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
      }

      assertCprAddressUpdatedEventPublished(personEntity.crn!!, originalMainAddress.updateId!!, null, CPR)
    }
  }

  @Test
  fun `cpr override isVerified and statusCode regardless of what SAS send`() {
    val personEntity = createPerson(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(
        Address(postcode = randomPostcode(), statusCode = P),
      ),
    )
    val originalAddressEntity = personEntity.addresses.first()
    createPersonKey().addPerson(personEntity)

    val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, originalAddressEntity).data
      .copy(typeVerified = false, statusCode = SasAddressStatus(P.name), startDate = randomDate())

    stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))

    publishSasAddressArrivedEvent(originalAddressEntity.updateId!!)

    awaitAssert {
      val actualAddressEntity = addressRepository.findByUpdateId(originalAddressEntity.updateId!!)!!
      assertThat(actualAddressEntity.isVerified).isEqualTo(true)
      assertThat(actualAddressEntity.statusCode).isEqualTo(M)
      assertThat(actualAddressEntity.startDate!!.toUkLocalDate()).isEqualTo(sasCallbackResponse.startDate)
    }

    assertCprAddressUpdatedEventPublished(personEntity.crn!!, originalAddressEntity.updateId!!, null, CPR)
  }

  @Nested
  inner class FailureScenarios {

    @Test
    fun `address not returned from sas - pushes event to dead letter queue`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails(),
        configure = addAddressToRecord(Address(postcode = randomPostcode(), statusCode = P)),
      )
      createPersonKey().addPerson(personEntity)

      stubGetRequestToSas(null, status = 404)
      publishSasAddressArrivedEvent(personEntity.addresses.first().updateId!!)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
      val actualAddress = personRepository.findByCrn(personEntity.crn!!)!!.addresses.first()
      assertThat(actualAddress.statusCode).isEqualTo(P)
    }

    @Test
    fun `cpr address does not exist - pushed to dead letter queue`() {
      val personEntity = createPerson(
        createRandomProbationPersonDetails(),
        configure = addAddressToRecord(Address(postcode = randomPostcode(), statusCode = P)),
      )
      createPersonKey().addPerson(personEntity)

      val sasCallbackResponse = createSasAddressGetResponse(personEntity.crn, personEntity.addresses.first()).data
        .copy(typeVerified = true, statusCode = SasAddressStatus(M.name), startDate = randomDate())

      stubGetRequestToSas(SasGetAddressResponse(sasCallbackResponse))
      publishSasAddressArrivedEvent(UUID.randomUUID())

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
      val actualAddress = personRepository.findByCrn(personEntity.crn!!)!!.addresses.first()
      assertThat(actualAddress.statusCode).isEqualTo(P)
    }
  }
}
