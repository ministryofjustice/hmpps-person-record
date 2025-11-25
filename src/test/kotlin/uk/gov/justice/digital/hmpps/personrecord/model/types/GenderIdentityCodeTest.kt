package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.GIF
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.GIM
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.GINB
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.GIRF
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.GISD
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.UNK
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.util.stream.Stream

class GenderIdentityCodeTest {

  @Test
  fun `should handle null probation gender identity code`() {
    val probationCase = ProbationCase(name = ProbationCaseName(firstName = randomName()), identifiers = Identifiers(crn = randomCrn()), genderIdentity = Value(null))
    val person = Person.from(probationCase)
    assertThat(person.genderIdentityCode).isNull()
  }

  @ParameterizedTest
  @MethodSource("probationGenderIdentityCodes")
  fun `should map probation gender identity codes to cpr gender identity codes`(probationGenderIdentityCode: String, cprGenderIdentityCode: GenderIdentityCode) {
    val probationCase = ProbationCase(name = ProbationCaseName(), identifiers = Identifiers(), genderIdentity = Value(probationGenderIdentityCode))
    assertThat(GenderIdentityCode.from(probationCase)).isEqualTo(cprGenderIdentityCode)
  }

  companion object {

    @JvmStatic
    fun probationGenderIdentityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("GIF", GIF),
      Arguments.of("GIM", GIM),
      Arguments.of("GINB", GINB),
      Arguments.of("GIRF", GIRF),
      Arguments.of("GISD", GISD),
      Arguments.of("UNK", UNK),
    )
  }
}
