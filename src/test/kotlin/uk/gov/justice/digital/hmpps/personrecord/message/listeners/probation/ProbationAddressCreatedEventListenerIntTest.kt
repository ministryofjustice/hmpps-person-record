package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class ProbationAddressCreatedEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `consuming address created event - saves address - triggers correct events`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressCreatedEvent(cprPerson.crn, probationAddress.deliusAddressId)

    assertAddressSaved(cprPerson.crn!!, probationAddress)
  }

  @Test
  fun `consuming address created event - address not retrieved from probation - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress, status = 404)
    publishProbationAddressCreatedEvent(cprPerson.crn, probationAddress.deliusAddressId)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address created event - cpr person does not exist - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress)
    publishProbationAddressCreatedEvent(randomCrn(), probationAddress.deliusAddressId)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  private fun randomProbationAddress(): ProbationAddress {
    val startDate = randomDate()
    val endDate = startDate.plusYears(10)
    return ProbationAddress(
      noFixedAbode = true,
      startDate = startDate,
      endDate = endDate,
      postcode = randomPostcode(),
      fullAddress = randomFullAddress(),
      buildingName = randomName(),
      addressNumber = randomAddressNumber(),
      streetName = randomLowerCaseString(),
      district = randomName(),
      townCity = randomName(),
      county = randomName(),
      uprn = randomUprn(),
      deliusAddressId = randomDigit().toLong(),
      isVerified = randomBoolean(),
      notes = randomLowerCaseString(),
      status = ProbationAddressStatus(AddressStatusCode.entries.random().name, "description"),
      usage = ProbationAddressUsage(AddressRecordType.entries.random().name, "description"),
      telephoneNumber = randomPhoneNumber(),
    )
  }

  private fun stubGetRequestToProbation(probationAddress: ProbationAddress, status: Int = 200) {
    stubGetRequest(
      url = "/address/${probationAddress.deliusAddressId}",
      status = status,
      body = probationAddress(
        address = ApiResponseSetupAddress(
          noFixedAbode = probationAddress.noFixedAbode,
          startDate = probationAddress.startDate,
          endDate = probationAddress.endDate,
          postcode = probationAddress.postcode,
          fullAddress = probationAddress.fullAddress,
          buildingName = probationAddress.buildingName,
          addressNumber = probationAddress.addressNumber,
          streetName = probationAddress.streetName,
          district = probationAddress.district,
          townCity = probationAddress.townCity,
          county = probationAddress.county,
          deliusAddressId = probationAddress.deliusAddressId!!,
          isVerified = probationAddress.isVerified,
          status = ApiResponseSetupAddressStatus(probationAddress.status?.code, probationAddress.status?.description),
          usage = ApiResponseSetupAddressUsage(probationAddress.usage?.code, probationAddress.usage?.description),
          uprn = probationAddress.uprn,
          notes = probationAddress.notes,
          telephoneNumber = probationAddress.telephoneNumber,
        ),
      ),
    )
  }

  private fun publishProbationAddressCreatedEvent(crn: String?, probationAddressId: Long?) {
    publishDomainEvent(
      OFFENDER_ADDRESS_CREATED,
      DomainEvent(
        eventType = OFFENDER_ADDRESS_CREATED,
        detailUrl = "/address/$probationAddressId",
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
      ),
    )
  }

  private fun assertAddressSaved(crn: String, probationAddress: ProbationAddress) {
    awaitAssert {
      val actualPersonEntity = personRepository.findByCrn(crn)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      assertThat(actualAddressEntity.updateId).isNotNull()
      assertThat(actualAddressEntity.updateId!!.toString()).isNotBlank
      assertThat(actualAddressEntity.noFixedAbode).isEqualTo(probationAddress.noFixedAbode)
      assertThat(actualAddressEntity.startDate).isEqualTo(probationAddress.startDate)
      assertThat(actualAddressEntity.endDate).isEqualTo(probationAddress.endDate)
      assertThat(actualAddressEntity.postcode).isEqualTo(probationAddress.postcode)
      assertThat(actualAddressEntity.fullAddress).isEqualTo(probationAddress.fullAddress)
      assertThat(actualAddressEntity.buildingName).isEqualTo(probationAddress.buildingName)
      assertThat(actualAddressEntity.postTown).isEqualTo(probationAddress.townCity)
      assertThat(actualAddressEntity.county).isEqualTo(probationAddress.county)
      assertThat(actualAddressEntity.uprn).isEqualTo(probationAddress.uprn)
      assertThat(actualAddressEntity.deliusAddressId).isEqualTo(probationAddress.deliusAddressId)
      assertThat(actualAddressEntity.isVerified).isEqualTo(probationAddress.isVerified)
      assertThat(actualAddressEntity.usages.first().usageCode).isEqualTo(AddressUsageCode.from(probationAddress.usage!!.code))
      assertThat(actualAddressEntity.statusCode).isEqualTo(AddressStatusCode.valueOf(probationAddress.status?.code!!))

      assertThat(actualAddressEntity.buildingNumber).isEqualTo(probationAddress.addressNumber)
      assertThat(actualAddressEntity.thoroughfareName).isEqualTo(probationAddress.streetName)
      assertThat(actualAddressEntity.dependentLocality).isEqualTo(probationAddress.district)
      assertThat(actualAddressEntity.comment).isEqualTo(probationAddress.notes)
      assertThat(actualAddressEntity.contacts.first().contactType).isEqualTo(ContactType.HOME)
      assertThat(actualAddressEntity.contacts.first().contactValue).isEqualTo(probationAddress.telephoneNumber)
    }
  }
}
