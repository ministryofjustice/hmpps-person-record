package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

class AddressStatusCodeTest {

  @Nested
  inner class Probation {
    @Test
    fun `should map known code string to status code`() {
      val code = randomAddressStatusCode()
      val parsedStatusCode = AddressStatusCode.fromProbation(code.name)
      assertThat(parsedStatusCode).isEqualTo(code)
    }

    @Test
    fun `should map random code string to UNKNOWN status code`() {
      val code = randomLowerCaseString()
      assertDoesNotThrow {
        val parsedStatusCode = AddressStatusCode.fromProbation(code)
        assertThat(parsedStatusCode).isEqualTo(AddressStatusCode.UNKNOWN)
      }
    }
  }

  @Nested
  inner class Prison {
    @Test
    fun `should map to PM code when primary and mail flags are true`() {
      val code = AddressStatusCode.fromPrison(isPrimary = true, isMail = true)
      assertThat(code).isEqualTo(AddressStatusCode.PM)
    }

    @Test
    fun `should map to M code when primary flag is true and mail flag is false`() {
      val code = AddressStatusCode.fromPrison(isPrimary = true, isMail = false)
      assertThat(code).isEqualTo(AddressStatusCode.M)
    }

    @Test
    fun `should map to MA code when primary flag is false and mail flag is true`() {
      val code = AddressStatusCode.fromPrison(isPrimary = false, isMail = true)
      assertThat(code).isEqualTo(AddressStatusCode.MA)
    }

    @Test
    fun `should map to null code when primary and mail flags are false`() {
      val code = AddressStatusCode.fromPrison(isPrimary = false, isMail = false)
      assertThat(code).isNull()
    }
  }
}
