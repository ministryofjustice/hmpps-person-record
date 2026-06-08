package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class AddressDomainEventPublisherE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var addressService: AddressService

  @Test
  fun `should publish a CPR address created domain event when an address is created in sas`() {
    val crn = randomCrn()
    val newAddress = createRandomProbationAddress()
    createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

    webTestClient
      .post()
      .uri(probationAddressApiUrl(crn))
      .headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = listOf(PROBATION_API_READ_WRITE)))
      .bodyValue(newAddress)
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody<ProbationCreateAddressResponse>()
      .returnResult().responseBody!!

    assertDomainEventPublishedAfterSasEvent(
      expectedEventType = CPR_PROBATION_ADDRESS_CREATED,
      crn = crn,
    )
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
