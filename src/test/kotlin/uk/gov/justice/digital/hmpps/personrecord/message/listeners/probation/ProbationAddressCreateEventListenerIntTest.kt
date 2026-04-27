package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
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
import uk.gov.justice.digital.hmpps.personrecord.test.responses.address
import java.util.UUID

class ProbationAddressCreateEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `consuming address created event - saves address - triggers correct events`() {
    val probationAddressId = randomDigit()
    stubGetRequestToProbation(probationAddressId)

    val crn = randomCrn()
    val existingPersonEntity = createPerson(createRandomProbationPersonDetails(crn = crn).copy(addresses = emptyList()))
    createPersonKey()
      .addPerson(existingPersonEntity)

    publishProbationDomainEvent(OFFENDER_ADDRESS_CREATED, crn, additionalInformation = AdditionalInformation(addressId = probationAddressId))

    awaitAssert {
      val actualPersonEntity = personRepository.findByCrn(crn)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      // ....

      assertPublishedDomainEvent(crn, actualAddressEntity.updateId!!, probationAddressId)
    }
  }

  private fun stubGetRequestToProbation(probationAddressId: String) {
    ApiResponseSetupAddress(
      addressId = probationAddressId,
      noFixedAbode = true,
      startDate = randomDate(),
      postcode = randomPostcode(),
      fullAddress = randomFullAddress(),
      buildingName = randomBuildingNumber(),
      addressNumber = randomAddressNumber(),
      streetName = randomLowerCaseString(),
      district = randomName(),
      townCity = randomName(),
      county = randomName(),
      uprn = randomUprn(),
      notes = randomName(),
      telephoneNumber = randomPhoneNumber(),
    ).also { stubGetRequest(url = "/person/address/${it.addressId}", body = address(it)) }
  }

  private fun assertPublishedDomainEvent(crn: String, cprAddressId: UUID, probationAddressId: String) {
    expectOneMessageOn(testOnlyCPREventsQueue)
    val actualDomainEvent = testOnlyCPREventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCPREventsQueue?.queueUrl).build())!!.get()
    val sqsMessage = actualDomainEvent.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    val domainEvent = jsonMapper.readValue<DomainEvent>(sqsMessage.message)

    assertThat(domainEvent.getCrn()).isEqualTo(crn)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/$cprAddressId")
    assertThat(domainEvent.additionalInformation?.deliusAddressId).isEqualTo(probationAddressId)
    assertThat(domainEvent.additionalInformation?.cprAddressId).isEqualTo(cprAddressId.toString())
  }
}
