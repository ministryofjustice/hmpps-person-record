package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationAddressCreatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address created event - saves address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(cprPerson.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    assertAddress(cprPerson.crn!!, probationAddress)
    assertDomainEventPublished(cprPerson.crn)
  }

  @Test
  fun `consuming address created event - address with delius address id already exists - updates address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val addressEntityBeforeCreateEvent = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val updatedProbationAddress = randomProbationAddress(originalProbationAddress.deliusAddressId)
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationAddressEvent(personEntity.crn, originalProbationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val addressEntityAfterCreateEvent = actualPersonEntity.addresses.first()
    assertThat(addressEntityAfterCreateEvent.id).isEqualTo(addressEntityBeforeCreateEvent.id)
    assertThat(addressEntityAfterCreateEvent.updateId).isEqualTo(addressEntityBeforeCreateEvent.updateId)
    assertAddress(personEntity.crn!!, updatedProbationAddress)

    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }

  @Test
  fun `consuming address created event - address not retrieved from probation - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress, status = 404)
    publishProbationAddressEvent(cprPerson.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address created event - cpr person does not exist - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress)
    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  private fun assertDomainEventPublished(crn: String) {
    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(crn) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val addressEntity = actualPersonEntity.addresses.first()

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_ADDRESS_CREATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(DELIUS.identifier))

    val domainEvent: DomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/${addressEntity.updateId}")
    assertThat(domainEvent.description).isEqualTo("A probation address has been created for a person")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference?.identifiers?.get(0)?.type).isEqualTo("CRN")
    assertThat(domainEvent.personReference?.identifiers?.get(0)?.value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation?.cprAddressId).isEqualTo(addressEntity.updateId.toString())
    assertThat(domainEvent.additionalInformation?.eventSource).isEqualTo(DomainEventSource.DELIUS.identifier)
    assertThat(domainEvent.additionalInformation?.outboundDeliusAddressId).isEqualTo(addressEntity.deliusAddressId.toString())
  }
}
