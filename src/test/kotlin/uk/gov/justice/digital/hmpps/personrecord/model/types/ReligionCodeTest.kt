package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ReligionCodeTest {

  @Test
  fun `returns description when code is recognised`() {
    val religionCodeEnum = ReligionCode.entries.random()
    assertThat(religionCodeEnum.name.toReligionCodeDescription()).isEqualTo(religionCodeEnum.description)
  }

  @Test
  fun `returns null when code is not recognised`() {
    assertDoesNotThrow { assertThat("FAKE_OR_OLD_CODE".toReligionCodeDescription()).isEqualTo(null) }
  }

  @Test
  fun `returns null when code is null`() {
    assertDoesNotThrow { assertThat(null.toReligionCodeDescription()).isEqualTo(null) }
  }
}
