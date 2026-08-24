package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
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
  fun `should keep original order for source systems`() {
    listOf(NOMIS, DELIUS, LIBRA).forEach { sourceSystem ->
      val first = address(randomPostcode())
      val second = address(randomPostcode())

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

  @Nested
  inner class Temp {
    @Test
    fun `no updates to address for person since flow change - maintains sorting by record type for existing addresses`() {
      val previous = address(randomPostcode())
      previous.recordType = AddressRecordType.PREVIOUS
      val primary = address(randomPostcode())
      primary.recordType = AddressRecordType.PRIMARY

      val sorted = CanonicalAddressSorter.sort(addressesFor(COMMON_PLATFORM, previous, primary))

      assertThat(sorted).containsExactly(primary, previous)
    }

    @Test
    fun `updates to address for person after flow change - sorts by status code`() {
      val previous = address(randomPostcode())
      previous.statusCode = AddressStatusCode.P
      previous.recordType = AddressRecordType.PREVIOUS
      val primary = address(randomPostcode())
      primary.statusCode = AddressStatusCode.M
      primary.recordType = AddressRecordType.PRIMARY

      val sorted = CanonicalAddressSorter.sort(addressesFor(COMMON_PLATFORM, previous, primary))

      assertThat(sorted).containsExactly(primary, previous)
    }
  }

  private fun addressesFor(sourceSystem: SourceSystemType, vararg addresses: AddressEntity): List<AddressEntity> {
    val person = PersonEntity(sourceSystem = sourceSystem, matchId = UUID.randomUUID())
    addresses.forEach { it.person = person }
    return addresses.toList()
  }

  private fun address(postcode: String, statusCode: AddressStatusCode? = null): AddressEntity = AddressEntity(
    updateId = UUID.randomUUID(),
    postcode = postcode,
    statusCode = statusCode,
  )
}
