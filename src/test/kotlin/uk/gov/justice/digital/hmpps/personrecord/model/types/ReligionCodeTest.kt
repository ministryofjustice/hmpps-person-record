package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode

class ReligionCodeTest {

  @Test
  fun `should map known religion code correctly`() {
    val validReligion = randomReligionCode().name
    val code = ReligionCode.fromProbation(validReligion)
    assertThat(code).isEqualTo(validReligion)
  }

  @Test
  fun `should map delius religion code -1 to UNKNOWN`() {
    val code = ReligionCode.fromProbation("-1")
    assertThat(code).isEqualTo(ReligionCode.UNKN.name)
  }

  @Test
  fun `should map delius religion code REL01 to TPRNTS`() {
    val code = ReligionCode.fromProbation("REL01")
    assertThat(code).isEqualTo(ReligionCode.TPRNTS.name)
  }

  @Test
  fun `should map null religion code to null`() {
    val code = ReligionCode.fromProbation(null)
    assertThat(code).isNull()
  }
}
