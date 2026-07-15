package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressData
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrived
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrivedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
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
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.Address as SasAddress

class ProbationEventListenerTestBase : MessagingMultiNodeTestBase() {

  fun randomProbationAddress(deliusAddressId: Long? = null): ProbationAddress {
    val startDateTime = randomDateTime()
    val endDateTime = startDateTime.plusYears(10)
    return ProbationAddress(
      noFixedAbode = true,
      startDateTime = startDateTime.toZonedDateTime(),
      endDateTime = endDateTime.toZonedDateTime(),
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
          startDateTime = probationAddress.startDateTime?.toLocalDateTime(),
          endDateTime = probationAddress.endDateTime?.toLocalDateTime(),
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

  fun publishProbationPersonCreatedEvent(crn: String) {
    publishDomainEvent(
      ProbationPersonCreated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
      ),
    )
  }

  fun publishProbationAddressCreatedEvent(crn: String?, cprAddressId: UUID?, deliusAddressId: Long?, eventSource: DomainEventSource) {
    publishDomainEvent(
      ProbationAddressCreated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationAddressCreatedInfo(
          cprAddressId = cprAddressId.toString(),
          deliusAddressId = deliusAddressId!!,
        ),
      ),
      eventSource,
    )
  }

  fun publishProbationAddressUpdatedEvent(crn: String?, deliusAddressId: Long?) {
    publishDomainEvent(
      ProbationAddressUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationAddressUpdatedInfo(
          deliusAddressId = deliusAddressId!!,
        ),
      ),
      DELIUS,
    )
  }

  fun publishProbationAddressDeletedEvent(crn: String?, deliusAddressId: Long?) {
    publishDomainEvent(
      ProbationAddressDeleted(
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
        additionalInformation = ProbationAddressDeletedInfo(
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

  fun publishSasAddressArrivedEvent(cprAddressId: UUID) {
    publishDomainEvent(
      SasAddressArrived(
        detailUrl = "/accommodations/1234",
        additionalInformation = SasAddressArrivedInfo(
          cprAddressId = cprAddressId.toString(),
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

  fun createSasAddressGetResponse(crn: String?, address: AddressEntity) = SasGetAddressResponse(
    data = SasAddressData(
      crn = crn!!,
      cprAddressId = address.updateId.toString(),
      startDate = LocalDate.now(),
      noFixedAbode = address.noFixedAbode,
      typeVerified = address.isVerified,
      address = SasAddress(
        postcode = address.postcode,
        subBuildingName = address.subBuildingName,
        buildingName = address.buildingName,
        buildingNumber = address.buildingNumber,
        thoroughfareName = address.thoroughfareName,
        dependentLocality = address.dependentLocality,
        postTown = address.postTown,
        county = address.county,
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
      assertThat(actualAddressEntity.startDate).isEqualTo(probationAddress.startDateTime?.toLocalDateTime())
      assertThat(actualAddressEntity.endDate).isEqualTo(probationAddress.endDateTime?.toLocalDateTime())
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

  fun assertCprAddressCreatedEventPublished(crn: String, cprAddressId: UUID) {
    val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
    assertThat(sqsMessage.getEventType()).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(DELIUS.identifier))
    val domainEvent: CprAddressCreated = jsonMapper.readValue<CprAddressCreated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/$cprAddressId")
    assertThat(domainEvent.description).isEqualTo("A probation address has been created for a person")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.first { it.type == "CRN" }?.value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation.cprAddressId).isEqualTo(cprAddressId)
    assertThat(domainEvent.additionalInformation.deliusAddressId).isNull()
  }

  fun assertCprAddressUpdatedEventPublished(crn: String, cprAddressId: UUID, deliusAddressId: Long?, eventSource: DomainEventSource) {
    val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
    assertThat(sqsMessage.getEventType()).isEqualTo(CPR_PROBATION_ADDRESS_UPDATED)
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(eventSource.identifier))
    val domainEvent: CprAddressUpdated = jsonMapper.readValue<CprAddressUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_UPDATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/$cprAddressId")
    assertThat(domainEvent.description).isEqualTo("A probation address has been updated for a person")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.first { it.type == "CRN" }?.value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation.cprAddressId).isEqualTo(cprAddressId)
    assertThat(domainEvent.additionalInformation.deliusAddressId).isEqualTo(deliusAddressId)
  }

  fun assertCprAddressDeletedEventPublished(crn: String, cprAddressId: UUID, deliusAddressId: Long?, eventSource: DomainEventSource) {
    val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
    assertThat(sqsMessage.getEventType()).isEqualTo(CPR_PROBATION_ADDRESS_DELETED)
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(eventSource.identifier))
    val domainEvent: CprAddressDeleted = jsonMapper.readValue<CprAddressDeleted>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_DELETED)
    assertThat(domainEvent.description).isEqualTo("A probation address has been deleted for a person")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.first { it.type == "CRN" }?.value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation.cprAddressId).isEqualTo(cprAddressId)
    assertThat(domainEvent.additionalInformation.deliusAddressId).isEqualTo(deliusAddressId)
  }

  fun assertNoCprActions(crn: String) {
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
