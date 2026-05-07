package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

class AddressUsageCodeTest {

  @Test
  fun `should map known code string to usage code`() {
    val code = randomAddressUsageCode()
    val parsedUsageCode = AddressUsageCode.from(code.name)
    assertThat(parsedUsageCode).isEqualTo(code)
  }

  @Test
  fun `should map random code string to UNKNOWN usage code`() {
    val code = randomLowerCaseString()
    assertDoesNotThrow {
      val parsedUsageCode = AddressUsageCode.from(code)
      assertThat(parsedUsageCode).isEqualTo(AddressUsageCode.UNKNOWN)
    }
  }
}
