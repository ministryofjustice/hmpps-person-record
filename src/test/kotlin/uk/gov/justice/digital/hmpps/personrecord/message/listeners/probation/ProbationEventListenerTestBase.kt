package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressData
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrived
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrivedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
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
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.Address as SasAddress

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

  fun publishProbationOffenderCreatedEvent(crn: String) {
    publishDomainEvent(
      ProbationOffenderCreated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
      ),
    )
  }

  fun publishProbationOffenderDeletedEvent(eventType: String, crn: String) {
    publishDomainEvent(
      ProbationOffenderDeleted(
        eventType = eventType,
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
      ),
    )
  }

  fun publishProbationOffenderAddressCreatedEvent(crn: String?, cprAddressId: UUID?, deliusAddressId: Long?, eventSource: DomainEventSource) {
    publishDomainEvent(
      ProbationOffenderAddressCreated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationOffenderAddressCreatedInfo(
          cprAddressId = cprAddressId.toString(),
          deliusAddressId = deliusAddressId!!,
        ),
      ),
      eventSource,
    )
  }

  fun publishProbationOffenderAddressUpdatedEvent(crn: String?, deliusAddressId: Long?) {
    publishDomainEvent(
      ProbationOffenderAddressUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationOffenderAddressUpdatedInfo(
          deliusAddressId = deliusAddressId!!,
        ),
      ),
      DELIUS,
    )
  }

  fun publishProbationAddressDeletedEvent(crn: String?, deliusAddressId: Long?) {
    publishDomainEvent(
      ProbationOffenderAddressDeleted(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationOffenderAddressDeletedInfo(
          deliusAddressId = deliusAddressId!!,
        ),
      ),
      eventSource = DELIUS,
    )
  }

  fun publishSasAddressUpdatedEvent() {
    publishDomainEvent(
      SasAddressUpdated(
        detailUrl = "/accommodations/1234",
      ),
    )
  }

  fun publishSasAddressArrivedEvent(cprAddressUpdateId: UUID?) {
    publishDomainEvent(
      SasAddressArrived(
        detailUrl = "/accommodations/1234",
        additionalInformation = SasAddressArrivedInfo(
          corePersonAddressId = cprAddressUpdateId.toString(),
        ),
      ),
    )
  }

  fun publishSasAddressDeletedEvent(cprAddressId: UUID) {
    publishDomainEvent(
      SasAddressDeleted(
        additionalInformation = SasAddressDeletedInfo(
          cprAddressId = cprAddressId.toString(),
        ),
      ),
    )
  }

  fun createSasAddressGetResponse(crn: String?, cprAddressUpdateId: UUID?) = SasGetAddressResponse(
    data = SasAddressData(
      crn = crn!!,
      cprAddressId = cprAddressUpdateId.toString(),
      startDate = LocalDate.now(),
      noFixedAbode = randomBoolean(),
      typeVerified = randomBoolean(),
      address = SasAddress(
        postcode = randomPostcode(),
        subBuildingName = randomName(),
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomPostcode(),
        county = randomName(),
        countryCode = "E",
        uprn = randomUprn(),
      ),
      statusCode = SasAddressStatus(
        code = randomAddressStatusCode().name,
        description = randomLowerCaseString(),
      ),
      usage = SasAddressType(
        code = randomAddressUsageCode().name,
        description = randomLowerCaseString(),
      ),
    ),
  )

  fun stubGetRequestToSas(
    sasCallbackResponse: SasGetAddressResponse? = null,
    status: Int = 200,
  ) {
    stubGetRequest(
      url = "/accommodations/1234",
      body = jsonMapper.writeValueAsString(sasCallbackResponse),
      status = status,
    )
  }

  fun assertAddress(crn: String, probationAddress: ProbationAddress): AddressEntity {
    var actualAddressEntity: AddressEntity? = null
    awaitAssert {
      val actualPersonEntity = personRepository.findByCrn(crn)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      actualAddressEntity = actualPersonEntity.addresses.first()
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
    return actualAddressEntity!!
  }

  fun assertDomainEventPublishedAfterDeliusEvent(expectedEventType: String, crn: String, cprAddressUpdateId: String) {
    val (_, domainEvent) = checkDomainEventPublished(crn, expectedEventType, cprAddressUpdateId, DELIUS)
    assertThat(domainEvent.additionalInformation?.outboundDeliusAddressId).isNull()
  }

  fun assertDomainEventPublishedAfterSasEvent(expectedEventType: String, crn: String, cprAddressUpdateId: String) {
    val (addressEntity, domainEvent) = checkDomainEventPublished(crn, expectedEventType, cprAddressUpdateId, CPR)
    assertThat(domainEvent.additionalInformation?.outboundDeliusAddressId).isEqualTo(addressEntity?.deliusAddressId)
  }

  private fun checkDomainEventPublished(
    crn: String,
    expectedEventType: String,
    cprAddressUpdateId: String,
    eventSource: DomainEventSource,
  ): Pair<AddressEntity?, DomainEvent> {
    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(crn) }
    val addressEntity = actualPersonEntity.addresses.firstOrNull()

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
    addressEntity?.let { assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/${addressEntity.updateId}") }
    assertThat(domainEvent.description).isEqualTo(expectedDescription(expectedEventType))
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.getCrn()).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation?.outboundCprAddressId).isEqualTo(cprAddressUpdateId)
    return Pair(addressEntity, domainEvent)
  }

  private fun expectedDescription(eventType: String): String = when (eventType) {
    CPR_PROBATION_ADDRESS_CREATED -> "A probation address has been created for a person"
    CPR_PROBATION_ADDRESS_UPDATED -> "A probation address has been updated for a person"
    CPR_PROBATION_ADDRESS_DELETED -> "A probation address has been deleted for a person"
    else -> error("Unsupported event type: $eventType")
  }

  fun assertNoCprActionsHappenAfterAddressPatch(crn: String) {
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
    assertNoCprActions(crn)
  }

  fun assertCorrectActionsHappenAfterSasAddressDelete(crn: String) {
    assertNoCprActions(crn)
  }

  private fun assertNoCprActions(crn: String) {
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_CREATED) { assertThat(it).isEmpty() }
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_UPDATED) { assertThat(it).isEmpty() }
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_DELETED) { assertThat(it).isEmpty() }

    checkTelemetry(
      event = TelemetryEventType.CPR_RECORD_UPDATED,
      expected = mapOf("SOURCE_SYSTEM" to DELIUS.name, "CRN" to crn),
      times = 0,
    )

    wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/person/score/.*")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/address/*")))
  }
}
