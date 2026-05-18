package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.AddressContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.Address as ProbationAddress

class AddressServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var addressService: AddressService

  @Autowired
  lateinit var addressRepository: AddressRepository

  @Nested
  inner class CreateAddress {

    @Test
    fun `should create address and recluster when matching fields have changed`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val addressToCreate = Address.from(createRandomProbationAddress())

      addressService.processAddress(
        addressToCreate,
        findPerson = { personRepository.findByCrn(crn) },
        findAddress = { null },
      )

      awaitAssert {
        val person = personRepository.findByCrn(crn)!!
        assertThat(person.addresses.size).isEqualTo(1)
        assertAddressValues(addressToCreate, person.addresses.first())
      }
    }

    @Test
    fun `should create address and not recluster when no matching fields have changed`() {
      val crn = randomCrn()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val addressToCreate = Address.from(createRandomProbationAddress().copy(postcode = null))

      addressService.processAddress(
        addressToCreate,
        findPerson = { personRepository.findByCrn(crn) },
        findAddress = { null },
      )

      awaitAssert {
        val person = personRepository.findByCrn(crn)!!
        assertThat(person.addresses.size).isEqualTo(1)
        assertAddressValues(addressToCreate, person.addresses.first())
      }
    }
  }

  @Nested
  inner class UpdateAddress {

    @Test
    fun `should update address and recluster when matching fields have changed`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      val initialAddress = Address.from(createRandomProbationAddress())
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = listOf(initialAddress)))
      val addressToCreate = initialAddress.copy(postcode = randomPostcode())

      addressService.processAddress(
        addressToCreate,
        findPerson = { personRepository.findByCrn(crn) },
        findAddress = { addressRepository.findByUpdateId(person.addresses[0].updateId!!) },
      )

      awaitAssert {
        val person = personRepository.findByCrn(crn)!!
        assertThat(person.addresses.size).isEqualTo(1)
        assertAddressValues(addressToCreate, person.addresses.first())
      }
    }

    @Test
    fun `should update address and not recluster when no matching fields have changed`() {
      val crn = randomCrn()
      val initialAddress = Address.from(createRandomProbationAddress())
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = listOf(initialAddress)))
      val addressToCreate = initialAddress.copy(buildingNumber = randomBuildingNumber())

      addressService.processAddress(
        addressToCreate,
        findPerson = { personRepository.findByCrn(crn) },
        findAddress = { addressRepository.findByUpdateId(person.addresses[0].updateId!!) },
      )

      awaitAssert {
        val person = personRepository.findByCrn(crn)!!
        assertThat(person.addresses.size).isEqualTo(1)
        assertAddressValues(addressToCreate, person.addresses.first())
      }
    }
  }

  private fun createRandomProbationAddress(): ProbationAddress = ProbationAddress(
    noFixedAbode = false,
    startDate = randomZonedDateTime(),
    endDate = randomZonedDateTime(),
    postcode = randomPostcode(),
    uprn = randomUprn(),
    subBuildingName = randomName(),
    buildingName = randomName(),
    buildingNumber = randomBuildingNumber(),
    thoroughfareName = randomName(),
    dependentLocality = randomName(),
    postTown = randomName(),
    county = randomName(),
    countryCode = randomCountryCode(),
    comment = randomName(),
    statusCode = randomAddressStatusCode(),
    usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean())),
    contacts = listOf(AddressContact(randomContactType(), randomPhoneNumber(), "44")),
  )

  private fun assertAddressValues(expectedAddress: Address, actualAddress: AddressEntity) {
    assertThat(actualAddress.updateId.toString()).isNotEmpty()
    assertThat(actualAddress.noFixedAbode).isEqualTo(expectedAddress.noFixedAbode)
    assertThat(actualAddress.startDate).isEqualTo(expectedAddress.startDate)
    assertThat(actualAddress.endDate).isEqualTo(expectedAddress.endDate)
    assertThat(actualAddress.postcode).isEqualTo(expectedAddress.postcode)
    assertThat(actualAddress.uprn).isEqualTo(expectedAddress.uprn)
    assertThat(actualAddress.subBuildingName).isEqualTo(expectedAddress.subBuildingName)
    assertThat(actualAddress.buildingName).isEqualTo(expectedAddress.buildingName)
    assertThat(actualAddress.buildingNumber).isEqualTo(expectedAddress.buildingNumber)
    assertThat(actualAddress.thoroughfareName).isEqualTo(expectedAddress.thoroughfareName)
    assertThat(actualAddress.dependentLocality).isEqualTo(expectedAddress.dependentLocality)
    assertThat(actualAddress.postTown).isEqualTo(expectedAddress.postTown)
    assertThat(actualAddress.county).isEqualTo(expectedAddress.county)
    assertThat(actualAddress.countryCode).isEqualTo(expectedAddress.countryCode)
    assertThat(actualAddress.comment).isEqualTo(expectedAddress.comment)
    assertThat(actualAddress.statusCode).isEqualTo(expectedAddress.statusCode)
    assertThat(actualAddress.usages.size).isEqualTo(expectedAddress.usages.size)
    expectedAddress.usages.zip(actualAddress.usages).forEach { (expected, actual) ->
      assertThat(actual.usageCode).isEqualTo(expected.addressUsageCode)
      assertThat(actual.active).isEqualTo(expected.isActive)
    }
    assertThat(actualAddress.contacts.size).isEqualTo(expectedAddress.contacts.size)
    expectedAddress.contacts.zip(actualAddress.contacts).forEach { (expected, actual) ->
      assertThat(actual.contactType).isEqualTo(expected.contactType)
      assertThat(actual.contactValue).isEqualTo(expected.contactValue)
      assertThat(actual.extension).isEqualTo(expected.extension)
    }
  }
}
