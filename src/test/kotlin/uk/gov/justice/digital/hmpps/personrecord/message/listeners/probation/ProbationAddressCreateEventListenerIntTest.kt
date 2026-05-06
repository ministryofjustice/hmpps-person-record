package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class ProbationAddressCreateEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `consuming address created event - saves address - triggers correct events`() {
    val probationAddressId = randomDigit()
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress, probationAddressId)

    publishProbationAddressCreatedEvent(cprPerson.crn, probationAddressId)

    assertAddressSaved(cprPerson.crn!!, probationAddress)
    assertPublishedDomainEvent(cprPerson.crn, probationAddressId)
  }

  @Test
  fun `consuming address created event - address not retrieved from probation - pushes message to dead letter queue`() {
    val probationAddressId = randomDigit()
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress, probationAddressId, status = 404)
    publishProbationAddressCreatedEvent(cprPerson.crn, probationAddressId)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    expectNoMessagesOn(testOnlyCPREventsQueue)
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
      notes = randomLowerCaseString(),
      status = ProbationAddressStatus(AddressStatusCode.entries.random().name, "description"),
      type = ProbationAddressType(AddressRecordType.entries.random().name, "description"),
      telephoneNumber = randomPhoneNumber(),
    )
  }

  private fun stubGetRequestToProbation(probationAddress: ProbationAddress, probationAddressId: String, status: Int = 200) {
    stubGetRequest(
      url = "/address/$probationAddressId",
      status = status,
      body = probationAddress(
        address = ApiResponseSetupAddress(
          addressId = probationAddressId,
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
          uprn = probationAddress.uprn,
          notes = probationAddress.notes,
          telephoneNumber = probationAddress.telephoneNumber,
        ),
      ),
    )
  }

  private fun publishProbationAddressCreatedEvent(crn: String?, probationAddressId: String?) {
    publishDomainEvent(
      OFFENDER_ADDRESS_CREATED,
      DomainEvent(
        eventType = OFFENDER_ADDRESS_CREATED,
        detailUrl = "/address/$probationAddressId",
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = AdditionalInformation(
          addressId = probationAddressId,
        ),
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

      assertThat(actualAddressEntity.buildingNumber).isEqualTo(probationAddress.addressNumber)
      assertThat(actualAddressEntity.thoroughfareName).isEqualTo(probationAddress.streetName)
      assertThat(actualAddressEntity.dependentLocality).isEqualTo(probationAddress.district)
      assertThat(actualAddressEntity.comment).isEqualTo(probationAddress.notes)
      assertThat(actualAddressEntity.contacts.first().contactType).isEqualTo(ContactType.HOME)
      assertThat(actualAddressEntity.contacts.first().contactValue).isEqualTo(probationAddress.telephoneNumber)
    }
  }

  private fun assertPublishedDomainEvent(crn: String, probationAddressId: String) {
    val cprAddressUpdateId = personRepository.findByCrn(crn)!!.addresses.first().updateId!!.toString()
    expectOneMessageOn(testOnlyCPREventsQueue)
    val actualDomainEvent = testOnlyCPREventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCPREventsQueue?.queueUrl).build())!!.get()
    val sqsMessage = actualDomainEvent.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    val domainEvent = jsonMapper.readValue<DomainEvent>(sqsMessage.message)

    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(domainEvent.personReference!!.identifiers!!.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers.first().type).isEqualTo("CRN")
    assertThat(domainEvent.personReference.identifiers.first().value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation?.cprAddressId).isEqualTo(cprAddressUpdateId)
    assertThat(domainEvent.additionalInformation?.deliusAddressId).isEqualTo(probationAddressId)
    assertThat(domainEvent.version).isEqualTo(1)
    assertThat(domainEvent.description).isEqualTo("Address was created in Core Person Record")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/$cprAddressUpdateId")
    assertThat(domainEvent.occurredAt).isNotBlank
  }
}
