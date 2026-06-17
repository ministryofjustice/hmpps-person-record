package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import java.util.UUID

class AddressDomainEventPublisherE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var addressService: AddressService

  @Test
  fun `should publish a CPR address created domain event when an address is created in sas`() {
    val crn = randomCrn()
    val newAddress = createRandomProbationAddress()
    createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

    val responseBody = webTestClient
      .post()
      .uri(probationAddressApiUrl(crn))
      .headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = listOf(PROBATION_API_READ_WRITE)))
      .bodyValue(newAddress)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody<ProbationCreateAddressResponse>()
      .returnResult().responseBody!!

    val createdAddress = addressRepository.findByUpdateId(UUID.fromString(responseBody.cprAddressId))

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    assertThat(rawDomainEventMessage?.get()?.messages()?.first()?.body()?.contains("cprAddressId")).isTrue()
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_ADDRESS_CREATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    assertThat(sqsMessage.message.contains("unmergedCRN")).isFalse()

    val domainEvent = jsonMapper.readValue(sqsMessage.message) as CprAddressCreated
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/${createdAddress?.updateId}")
    assertThat(domainEvent.description).isEqualTo("A probation address has been created for a person")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation.cprAddressId).isEqualTo(createdAddress?.updateId.toString())
    assertThat(domainEvent.additionalInformation.deliusAddressId).isNull()
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class FeatureFlagPreprod {
    @Test
    fun `should not publish a CPR address created domain event in preprod`() {
      val crn = randomCrn()
      val newAddress = Address.from(createRandomProbationAddress())
      val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      addressService.processAddress(
        address = newAddress,
        findPerson = { personEntity },
        findAddress = { null },
        eventSource = CPR,
      )

      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  @Nested
  @ActiveProfiles("prod")
  inner class FeatureFlagProd {
    @Test
    fun `should not publish a CPR address created domain event in prod`() {
      val crn = randomCrn()
      val newAddress = Address.from(createRandomProbationAddress())
      val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      addressService.processAddress(
        address = newAddress,
        findPerson = { personEntity },
        findAddress = { null },
        eventSource = CPR,
      )

      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  private fun probationAddressApiUrl(crn: String) = "/person/probation/$crn/address"
}
