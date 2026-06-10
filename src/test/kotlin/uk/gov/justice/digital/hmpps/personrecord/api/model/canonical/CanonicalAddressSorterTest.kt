package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PREVIOUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class CanonicalAddressSorterTest {

  @Test
  fun `should order common platform addresses primary first then previous preserving original order`() {
    val previousOne = address(randomPostcode(), PREVIOUS)
    val primary = address(randomPostcode(), PRIMARY)
    val previousTwo = address(randomPostcode(), PREVIOUS)

    val sorted = CanonicalAddressSorter.sort(addressesFor(COMMON_PLATFORM, previousOne, primary, previousTwo))

    assertThat(sorted).containsExactly(primary, previousOne, previousTwo)
  }

  @Test
  fun `should place common platform addresses with no record type last`() {
    val noRecordType = address(randomPostcode(), recordType = null)
    val previous = address(randomPostcode(), PREVIOUS)
    val primary = address(randomPostcode(), PRIMARY)

    val sorted = CanonicalAddressSorter.sort(addressesFor(COMMON_PLATFORM, noRecordType, previous, primary))

    assertThat(sorted).containsExactly(primary, previous, noRecordType)
  }

  @Test
  fun `should keep original order for source systems whose addresses never have a record type`() {
    listOf(NOMIS, DELIUS, LIBRA).forEach { sourceSystem ->
      val first = address(randomPostcode(), recordType = null)
      val second = address(randomPostcode(), recordType = null)

      val sorted = CanonicalAddressSorter.sort(addressesFor(sourceSystem, first, second))

      assertThat(sorted)
        .`as`("ordering should be untouched for $sourceSystem")
        .containsExactly(first, second)
    }
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

  private fun address(postcode: String, recordType: AddressRecordType?): AddressEntity = AddressEntity(
    updateId = UUID.randomUUID(),
    postcode = postcode,
    recordType = recordType,
  )
}
