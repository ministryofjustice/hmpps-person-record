package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Address as SysconAddress

class AddressTest {

  @Test
  fun `SYSCON - standardized address builds from address correctly`() {
    val sysconAddress = buildSysconAddress()
    val actualAddress = Address.from(sysconAddress)
    assertThat(actualAddress.noFixedAbode).isEqualTo(sysconAddress.noFixedAbode)
    assertThat(actualAddress.startDate).isEqualTo(sysconAddress.startDate)
    assertThat(actualAddress.endDate).isEqualTo(sysconAddress.endDate)
    assertThat(actualAddress.postcode).isEqualTo(sysconAddress.postcode)
    assertThat(actualAddress.fullAddress).isEqualTo(sysconAddress.fullAddress)
    assertThat(actualAddress.subBuildingName).isEqualTo(sysconAddress.subBuildingName)
    assertThat(actualAddress.buildingName).isEqualTo(sysconAddress.buildingName)
    assertThat(actualAddress.buildingNumber).isEqualTo(sysconAddress.buildingNumber)
    assertThat(actualAddress.thoroughfareName).isEqualTo(sysconAddress.thoroughfareName)
    assertThat(actualAddress.dependentLocality).isEqualTo(sysconAddress.dependentLocality)
    assertThat(actualAddress.postTown).isEqualTo(sysconAddress.postTown)
    assertThat(actualAddress.county).isEqualTo(sysconAddress.county)
    assertThat(actualAddress.countryCode).isEqualTo(sysconAddress.countryCode)
    assertThat(actualAddress.comment).isEqualTo(sysconAddress.comment)
    assertThat(actualAddress.recordType).isEqualTo(AddressRecordType.PRIMARY)
  }

  @Test
  fun `SYSCON - isPrimary equals null - sets null`() {
    val sysconAddress = buildSysconAddress().copy(isPrimary = null)
    val actualAddress = Address.from(sysconAddress)
    assertThat(actualAddress.recordType).isNull()
  }

  @Test
  fun `SYSCON - isPrimary equals true - sets PRIMARY`() {
    val sysconAddress = buildSysconAddress().copy(isPrimary = true)
    val actualAddress = Address.from(sysconAddress)
    assertThat(actualAddress.recordType).isEqualTo(AddressRecordType.PRIMARY)
  }

  @Test
  fun `SYSCON - isPrimary equals true - sets PREVIOUS`() {
    val sysconAddress = buildSysconAddress().copy(isPrimary = false)
    val actualAddress = Address.from(sysconAddress)
    assertThat(actualAddress.recordType).isEqualTo(AddressRecordType.PREVIOUS)
  }

  private fun buildSysconAddress(
    isPrimary: Boolean = true,
  ) = SysconAddress(
    fullAddress = randomFullAddress(),
    noFixedAbode = randomBoolean(),
    startDate = LocalDate.now().minusYears((1..25).random().toLong()),
    endDate = LocalDate.now().plusYears((1..25).random().toLong()),
    postcode = randomPostcode(),
    subBuildingName = randomName(),
    buildingName = randomName(),
    buildingNumber = randomName(),
    thoroughfareName = randomName(),
    dependentLocality = randomName(),
    postTown = randomName(),
    county = randomName(),
    countryCode = CountryCode.entries.random().name,
    comment = randomName(),
    isPrimary = isPrimary,
    isMail = randomBoolean(),
    addressUsage = emptyList(),
  )
}
