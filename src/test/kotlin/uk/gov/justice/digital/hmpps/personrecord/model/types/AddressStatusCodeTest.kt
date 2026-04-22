package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AddressStatusCodeTest {

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
