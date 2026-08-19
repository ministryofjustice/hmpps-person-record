package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class CanonicalAddressSorterTest {

  @Test
  fun `should order common platform addresses main first then previous preserving original order`() {
    val previousOne = address(randomPostcode(), AddressStatusCode.P)
    val main = address(randomPostcode(), AddressStatusCode.M)
    val previousTwo = address(randomPostcode(), AddressStatusCode.P)

    val sorted = CanonicalAddressSorter.sort(addressesFor(COMMON_PLATFORM, previousOne, main, previousTwo))

    assertThat(sorted).containsExactly(main, previousOne, previousTwo)
  }

  @Test
  fun `should return empty list unchanged`() {
    assertThat(CanonicalAddressSorter.sort(emptyList())).isEmpty()
  }

  private fun addressesFor(sourceSystem: SourceSystemType, vararg addresses: AddressEntity): List<AddressEntity> {
    val person = PersonEntity(sourceSystem = sourceSystem, matchId = UUID.randomUUID())
    addresses.forEach { it.person = person }
    return addresses.toList()
  }

  private fun address(postcode: String, statusCode: AddressStatusCode): AddressEntity = AddressEntity(
    updateId = UUID.randomUUID(),
    postcode = postcode,
    statusCode = statusCode,
  )
}
