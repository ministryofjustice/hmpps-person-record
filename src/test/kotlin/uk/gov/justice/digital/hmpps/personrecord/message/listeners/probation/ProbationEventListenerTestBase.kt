package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class ProbationEventListenerTestBase : MessagingMultiNodeTestBase() {

  fun randomProbationAddress(deliusAddressId: Long? = null): ProbationAddress {
    val startDateTime = randomZonedDateTime()
    val endDateTime = startDateTime.plusYears(10)
    return ProbationAddress(
      noFixedAbode = true,
      startDateTime = startDateTime,
      endDateTime = endDateTime,
      postcode = randomPostcode(),
      fullAddress = randomFullAddress(),
      buildingName = randomName(),
      addressNumber = randomAddressNumber(),
      streetName = randomLowerCaseString(),
      district = randomName(),
      townCity = randomName(),
      county = randomName(),
      uprn = randomUprn(),
      deliusAddressId = deliusAddressId ?: randomDigit().toLong(),
      isVerified = randomBoolean(),
      notes = randomLowerCaseString(),
      status = ProbationAddressStatus(AddressStatusCode.entries.random().name, "description"),
      usage = ProbationAddressUsage(AddressUsageCode.entries.random().name, "description"),
      telephoneNumber = randomPhoneNumber(),
    )
  }

  fun stubGetRequestToProbation(probationAddress: ProbationAddress, status: Int = 200) {
    stubGetRequest(
      url = "/address/${probationAddress.deliusAddressId}",
      status = status,
      body = probationAddress(
        address = ApiResponseSetupAddress(
          noFixedAbode = probationAddress.noFixedAbode,
          startDateTime = probationAddress.startDateTime,
          endDateTime = probationAddress.endDateTime,
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

  fun publishProbationAddressEvent(crn: String?, probationAddressId: Long?, eventType: String, eventSource: DomainEventSource? = DELIUS, cprUpdateId: String? = null) {
    publishDomainEvent(
      eventType,
      DomainEvent(
        eventType = eventType,
        detailUrl = "/address/$probationAddressId",
        additionalInformation = AdditionalInformation(inboundCprAddressId = cprUpdateId, inboundDeliusAddressId = probationAddressId.toString()),
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
      ),
      eventSource,
    )
  }

  fun assertAddress(crn: String, probationAddress: ProbationAddress) {
    awaitAssert {
      val actualPersonEntity = personRepository.findByCrn(crn)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      assertThat(actualAddressEntity.updateId).isNotNull()
      assertThat(actualAddressEntity.updateId!!.toString()).isNotBlank
      assertThat(actualAddressEntity.noFixedAbode).isEqualTo(probationAddress.noFixedAbode)
      assertThat(actualAddressEntity.startDate).isEqualTo(probationAddress.startDateTime)
      assertThat(actualAddressEntity.endDate).isEqualTo(probationAddress.endDateTime)
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

  fun assertDomainEventPublishedAfterDeliusEvent(expectedEventType: String, crn: String) {
    val (addressEntity, domainEvent: DomainEvent) = checkDomainEventPublished(crn, expectedEventType, DELIUS)
    assertThat(domainEvent.additionalInformation?.outboundDeliusAddressId).isEqualTo(addressEntity.deliusAddressId.toString())
  }

  fun assertDomainEventPublishedAfterSasEvent(expectedEventType: String, crn: String) = checkDomainEventPublished(crn, expectedEventType, CPR)

  private fun checkDomainEventPublished(
    crn: String,
    expectedEventType: String,
    eventSource: DomainEventSource,
  ): Pair<AddressEntity, DomainEvent> {
    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(crn) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val addressEntity = actualPersonEntity.addresses.first()

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage =
      rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.getEventType()).isEqualTo(expectedEventType)
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(eventSource.identifier))

    val domainEvent: DomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(expectedEventType)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/${addressEntity.updateId}")
    assertThat(domainEvent.description).isEqualTo(expectedDescription(expectedEventType))
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.getCrn()).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation?.outboundCprAddressId).isEqualTo(addressEntity.updateId.toString())
    return Pair(addressEntity, domainEvent)
  }

  private fun expectedDescription(eventType: String): String = when (eventType) {
    CPR_PROBATION_ADDRESS_CREATED -> "A probation address has been created for a person"
    CPR_PROBATION_ADDRESS_UPDATED -> "A probation address has been updated for a person"
    else -> error("Unsupported event type: $eventType")
  }
}
