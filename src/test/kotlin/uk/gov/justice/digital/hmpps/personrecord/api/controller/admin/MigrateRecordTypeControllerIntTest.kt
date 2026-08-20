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
  fun `populates CP address status codes that are null if record type is not null`() {
    val p1 = createRandomCommonPlatformPersonDetails()
    p1.addresses = listOf(address(AddressRecordType.PRIMARY), address(AddressRecordType.PREVIOUS))
    val p2 = createRandomCommonPlatformPersonDetails()
    p2.addresses = listOf(address(null))

    createPersonKey().addPerson(p1)
    createPersonKey().addPerson(p2)

    sendPostRequestAsserted<String>(
      url = "/admin/migrate-record-types",
      expectedStatus = HttpStatus.OK,
      body = "",
      roles = emptyList(),
    )

    awaitAssert {
      val person1 = personRepository.findByDefendantId(p1.defendantId!!)!!
      val person2 = personRepository.findByDefendantId(p2.defendantId!!)!!

      assertThat(person1.addresses.first().statusCode).isEqualTo(AddressStatusCode.M)
      assertThat(person1.addresses.first().recordType).isEqualTo(AddressRecordType.PRIMARY)
      assertThat(person1.addresses.last().statusCode).isEqualTo(AddressStatusCode.P)
      assertThat(person1.addresses.last().recordType).isEqualTo(AddressRecordType.PREVIOUS)

      assertThat(person2.addresses.first().statusCode).isNull()
      assertThat(person2.addresses.first().recordType).isNull()
    }
  }

  private fun address(addressRecordType: AddressRecordType?) = Address(
    postcode = randomPostcode(),
    buildingName = randomName(),
    buildingNumber = randomBuildingNumber(),
    thoroughfareName = randomName(),
    dependentLocality = randomName(),
    postTown = randomPostcode(),
    recordType = addressRecordType,
  )
}
