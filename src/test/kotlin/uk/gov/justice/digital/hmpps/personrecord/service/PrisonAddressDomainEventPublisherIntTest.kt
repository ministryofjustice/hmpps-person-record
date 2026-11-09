package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonAddressesHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCreateAddressRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate

class PrisonAddressDomainEventPublisherIntTest(
  @Autowired private val prisonAddressesHandler: PrisonAddressesHandler,
) : MessagingTestBase() {

  @Test
  fun `should publish a CPR prison address created domain event when an address is created through CPR`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber).copy(addresses = emptyList()))

    val cprAddressId = prisonAddressesHandler.create(prisonNumber, addressRequest()).cprAddressId

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_ADDRESS_CREATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(CPR.identifier))
    val domainEvent: CprAddressCreated = jsonMapper.readValue<CprAddressCreated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_ADDRESS_CREATED)
    assertThat(domainEvent.description).isEqualTo("A prison address has been created for a person")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber/addresses/$cprAddressId")
    assertThat(domainEvent.additionalInformation.cprAddressId.toString()).isEqualTo(cprAddressId)
    assertThat(domainEvent.additionalInformation.deliusAddressId).isNull()
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  private fun addressRequest() = PrisonCreateAddressRequest(
    buildingName = "102",
    thoroughfareName = "Slinn Street",
    postTown = "Liverpool",
    countryCode = CountryCode.ENG,
    postcode = "LI1 5TH",
    statusCode = M,
    noFixedAbode = false,
    startDate = LocalDate.of(2020, 1, 1),
    usages = listOf(AddressUsageCode.CURFEW),
    userId = "A user",
  )
}
