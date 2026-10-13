package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.MAIL_TYPES
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.PRIMARY_TYPES
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.removeFlags
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

  @Nested
  inner class Types {
    @Test
    fun `primary types should contain M and PM`() {
      assertThat(PRIMARY_TYPES).containsExactlyInAnyOrder(AddressStatusCode.M, AddressStatusCode.PM)
    }

    @Test
    fun `mail types should contain MA and PM`() {
      assertThat(MAIL_TYPES).containsExactlyInAnyOrder(AddressStatusCode.MA, AddressStatusCode.PM)
    }
  }

  @Nested
  inner class RemoveFlags {
    @ParameterizedTest(name = "{0} removing primary={1} mail={2} should become {3}")
    @CsvSource(
      nullValues = ["null"],
      value = [
        "PM, true, true, null",
        "PM, true, false, MA",
        "PM, false, true, M",
        "PM, false, false, PM",
        "M, true, true, null",
        "M, true, false, null",
        "M, false, true, M",
        "M, false, false, M",
        "MA, true, true, null",
        "MA, true, false, MA",
        "MA, false, true, null",
        "MA, false, false, MA",
      ],
    )
    fun `should remove requested flags from prison status codes`(
      statusCode: AddressStatusCode,
      primary: Boolean,
      mail: Boolean,
      expected: AddressStatusCode?,
    ) {
      assertThat(statusCode.removeFlags(primary = primary, mail = mail)).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(AddressStatusCode::class, mode = EnumSource.Mode.EXCLUDE, names = ["M", "MA", "PM"])
    fun `should leave non prison status codes unchanged`(statusCode: AddressStatusCode) {
      assertThat(statusCode.removeFlags(primary = true, mail = true)).isEqualTo(statusCode)
    }

    @Test
    fun `should leave null status code as null`() {
      val statusCode: AddressStatusCode? = null
      assertThat(statusCode.removeFlags(primary = true, mail = true)).isNull()
    }
  }
}
