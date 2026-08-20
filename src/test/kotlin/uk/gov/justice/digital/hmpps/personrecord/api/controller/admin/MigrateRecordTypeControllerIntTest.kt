package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class MigrateRecordTypeControllerIntTest : WebTestBase() {

  @Test
  fun `populates CP address status codes that are null`() {
    val person = createRandomCommonPlatformPersonDetails()
    person.addresses = listOf(address(AddressRecordType.PREVIOUS), address(AddressRecordType.PRIMARY))
    createPersonKey().addPerson(person)

    sendPostRequestAsserted<String>(
      url = "/admin/migrate-record-types",
      expectedStatus = HttpStatus.OK,
      body = """{ "personBatchSize": 3 }""",
      roles = emptyList(),
    )

    awaitAssert {
      val addresses = personRepository.findByDefendantId(person.defendantId!!)!!.addresses.sortedBy { it.id }
      val previousAddress = addresses.first()
      val mainAddress = addresses.last()
      assertThat(previousAddress.statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(previousAddress.recordType).isEqualTo(AddressRecordType.PREVIOUS)
      assertThat(mainAddress.statusCode).isEqualTo(AddressStatusCode.M)
      assertThat(mainAddress.recordType).isEqualTo(AddressRecordType.PRIMARY)
    }
  }

  @Test
  fun `set the latest inserted address as main and others as previous when person has an address with null record type`() {
    val person = createRandomCommonPlatformPersonDetails()
    person.addresses = listOf(address(null), address(null))
    createPersonKey().addPerson(person)

    sendPostRequestAsserted<String>(
      url = "/admin/migrate-record-types",
      expectedStatus = HttpStatus.OK,
      body = """{ "personBatchSize": 3 }""",
      roles = emptyList(),
    )

    awaitAssert {
      val addresses = personRepository.findByDefendantId(person.defendantId!!)!!.addresses.sortedBy { it.id }
      val previousAddress = addresses.first()
      val mainAddress = addresses.last()
      assertThat(previousAddress.statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(previousAddress.recordType).isEqualTo(null)
      assertThat(mainAddress.statusCode).isEqualTo(AddressStatusCode.M)
      assertThat(mainAddress.recordType).isEqualTo(null)
    }
  }

  @Test
  fun `handles migrations that exceed batch size`() {
    val person1 = createRandomCommonPlatformPersonDetails()
    person1.addresses = listOf(address(AddressRecordType.PREVIOUS), address(AddressRecordType.PRIMARY))
    createPersonKey().addPerson(person1)

    val person2 = createRandomCommonPlatformPersonDetails()
    person2.addresses = listOf(address(AddressRecordType.PREVIOUS), address(AddressRecordType.PRIMARY))
    createPersonKey().addPerson(person2)

    val person3 = createRandomCommonPlatformPersonDetails()
    person3.addresses = listOf(address(AddressRecordType.PREVIOUS), address(AddressRecordType.PRIMARY))
    createPersonKey().addPerson(person3)

    sendPostRequestAsserted<String>(
      url = "/admin/migrate-record-types",
      expectedStatus = HttpStatus.OK,
      body = """{ "personBatchSize": 2 }""",
      roles = emptyList(),
    )

    awaitAssert {
      val personOneAddresses = personRepository.findByDefendantId(person1.defendantId!!)!!.addresses.sortedBy { it.id }
      assertThat(personOneAddresses.first().statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(personOneAddresses.last().statusCode).isEqualTo(AddressStatusCode.M)

      val personTwoAddresses = personRepository.findByDefendantId(person2.defendantId!!)!!.addresses.sortedBy { it.id }
      assertThat(personTwoAddresses.first().statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(personTwoAddresses.last().statusCode).isEqualTo(AddressStatusCode.M)

      val personThreeAddresses = personRepository.findByDefendantId(person3.defendantId!!)!!.addresses.sortedBy { it.id }
      assertThat(personThreeAddresses.first().statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(personThreeAddresses.last().statusCode).isEqualTo(AddressStatusCode.M)
    }
  }

  private fun address(addressRecordType: AddressRecordType?, postCode: String = randomPostcode()) = Address(
    postcode = postCode,
    buildingName = randomName(),
    buildingNumber = randomBuildingNumber(),
    thoroughfareName = randomName(),
    dependentLocality = randomName(),
    postTown = randomPostcode(),
    recordType = addressRecordType,
  )
}
