package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
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
  fun `should publish a CPR address created domain event when an address is created`() {
    val crn = randomCrn()
    val newAddress = Address.from(createRandomProbationAddress())
    val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

    addressService.processAddress(
      address = newAddress,
      findPerson = { personEntity },
      findAddress = { null },
      eventSource = CPR,
    )

    checkDomainEventPublished(
      crn = crn,
      expectedEventType = CPR_PROBATION_ADDRESS_CREATED,
      eventSource = CPR,
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
}
