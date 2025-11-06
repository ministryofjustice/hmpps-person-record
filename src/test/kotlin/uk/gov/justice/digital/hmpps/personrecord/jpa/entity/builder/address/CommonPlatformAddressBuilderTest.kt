package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class CommonPlatformAddressBuilderTest {

  @Test
  fun `returns all addresses if null passed`() {
    val existingAddress = Address(postcode = randomPostcode())
    val addresses = listOf(AddressEntity.from(existingAddress))
    val extract = CommonPlatformAddressBuilder.removePrimaryAddress(addresses, newAddress = null)
    assertThat(extract?.size).isEqualTo(1)
    assertThat(extract!![0]).isEqualTo(existingAddress)
  }

  @Test
  fun `returns no addresses if the only address is the same as the new one `() {
  }

  @Test
  fun `returns all addresses except the new one`() {
  }

  @Test
  fun `returns all addresses if none match the new one`() {
  }
}
