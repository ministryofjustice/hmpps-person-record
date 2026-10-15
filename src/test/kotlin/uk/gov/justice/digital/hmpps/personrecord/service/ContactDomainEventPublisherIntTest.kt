package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonContactsHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_CONTACT_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_CONTACT_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class ContactDomainEventPublisherIntTest(
  @Autowired private val prisonContactsHandler: PrisonContactsHandler,
) : MessagingTestBase() {

  @Test
  fun `should publish a CPR contact created domain event when a contact is created through CPR`() {
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))

    val cprContactId = prisonContactsHandler.create(prisonNumber, contactRequest()).contactId

    val sqsMessage = receiveDomainEvent()
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_CONTACT_CREATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    val domainEvent: CprContactCreated = jsonMapper.readValue<CprContactCreated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_CONTACT_CREATED)
    assertThat(domainEvent.description).isEqualTo("A prison contact has been created for a person")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber/contacts/$cprContactId")
    assertThat(domainEvent.additionalInformation.cprContactId.toString()).isEqualTo(cprContactId)
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  @Test
  fun `should publish a CPR contact updated domain event when a contact is updated through CPR`() {
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(
      createRandomPrisonPersonDetails(prisonNumber).copy(contacts = listOf(Contact(contactType = HOME, contactValue = "01234 111111"))),
    )
    val cprContactId = personRepository.findByPrisonNumber(prisonNumber)!!.contacts.single().updateId.toString()

    prisonContactsHandler.update(prisonNumber, cprContactId, contactRequest())

    val sqsMessage = receiveDomainEvent()
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_CONTACT_UPDATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    val domainEvent: CprContactUpdated = jsonMapper.readValue<CprContactUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_CONTACT_UPDATED)
    assertThat(domainEvent.description).isEqualTo("A prison contact has been updated for a person")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber/contacts/$cprContactId")
    assertThat(domainEvent.additionalInformation.cprContactId.toString()).isEqualTo(cprContactId)
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  @Test
  fun `should publish a CPR contact updated domain event when an address phone number is updated through CPR`() {
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(
      createRandomPrisonPersonDetails(prisonNumber).copy(
        addresses = listOf(Address(postcode = "AB1 1AA", contacts = listOf(Contact(contactType = HOME, contactValue = "01234 111111")))),
        contacts = emptyList(),
      ),
    )
    val addressEntity = personRepository.findByPrisonNumber(prisonNumber)!!.addresses.single()
    val addressId = addressEntity.updateId.toString()
    val cprContactId = addressEntity.contacts.single().updateId.toString()

    prisonContactsHandler.updateContact(prisonNumber, addressId, cprContactId, contactRequest())

    val sqsMessage = receiveDomainEvent()
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_CONTACT_UPDATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    val domainEvent: CprContactUpdated = jsonMapper.readValue<CprContactUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_CONTACT_UPDATED)
    assertThat(domainEvent.description).isEqualTo("A prison contact has been updated for a person")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber/contacts/$cprContactId")
    assertThat(domainEvent.additionalInformation.cprContactId.toString()).isEqualTo(cprContactId)
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  @Test
  fun `should publish a CPR contact created domain event for each address phone number created through CPR`() {
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(
      createRandomPrisonPersonDetails(prisonNumber).copy(addresses = listOf(Address(postcode = "AB1 1AA")), contacts = emptyList()),
    )
    val addressId = personRepository.findByPrisonNumber(prisonNumber)!!.addresses.single().updateId.toString()

    val cprContactIds = prisonContactsHandler.createContacts(
      prisonNumber,
      addressId,
      listOf(
        contactRequest().copy(type = HOME, value = "01234 111111"),
        contactRequest().copy(type = MOBILE, value = "07777 222222"),
      ),
    ).map { it.contactId }

    val sqsMessages = receiveDomainEvents(2)
    assertThat(sqsMessages).allSatisfy { sqsMessage ->
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_CONTACT_CREATED))
      assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    }
    val domainEvents = sqsMessages.map { jsonMapper.readValue<CprContactCreated>(it.message) }
    assertThat(domainEvents.map { it.additionalInformation.cprContactId.toString() }).containsExactlyInAnyOrderElementsOf(cprContactIds)
    assertThat(domainEvents).allSatisfy { domainEvent ->
      assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_CONTACT_CREATED)
      assertThat(domainEvent.description).isEqualTo("A prison contact has been created for a person")
      assertThat(domainEvent.detailUrl)
        .isEqualTo("http://localhost:8080/person/prison/$prisonNumber/contacts/${domainEvent.additionalInformation.cprContactId}")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
    }
  }

  private fun receiveDomainEvents(expectedCount: Int): List<SQSMessage> {
    expectMessageCountMessagesOn(testOnlyCPRDomainEventsQueue, expectedCount)
    val messages = mutableListOf<SQSMessage>()
    await untilCallTo {
      testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).maxNumberOfMessages(10).build(),
      )?.get()?.messages()?.forEach { messages.add(jsonMapper.readValue<SQSMessage>(it.body())) }
      messages.size
    } matches { it == expectedCount }
    return messages
  }

  private fun receiveDomainEvent(): SQSMessage = receiveDomainEvents(1).single()

  private fun contactRequest() = PrisonContactRequest(
    type = BUS,
    value = "01234 567 890",
    extension = "123",
    userId = "A user",
  )
}
