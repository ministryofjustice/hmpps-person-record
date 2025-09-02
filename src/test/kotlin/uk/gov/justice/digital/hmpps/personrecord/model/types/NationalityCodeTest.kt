package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.AG
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ALBA
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ALGE
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ANDO
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ANGOL
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ANTIG
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ARGEN
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.ASM
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.UNKNOWN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.stream.Stream

class NationalityCodeTest {

  @Test
  fun `should handle null probation nationality code`() {
    val probationCase = ProbationCase(name = ProbationCaseName(firstName = randomName()), identifiers = Identifiers(crn = randomCrn()), nationality = Value(null))
    val person = Person.from(probationCase)
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("probationCodes")
  fun `should map probation nationality codes to cpr nationality codes`(probationCode: String, cprCode: NationalityCode) {
    val probationCase = ProbationCase(name = ProbationCaseName(firstName = randomName()), identifiers = Identifiers(crn = randomCrn()), nationality = Value(probationCode))
    val person = Person.from(probationCase)
    assertThat(person.nationalities.first().code).isEqualTo(cprCode)
  }

  @Test
  fun `handle null Prison nationality code`() {
    val prisoner = Prisoner(prisonNumber = randomPrisonNumber(), firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate(), nationality = null)
    val person = Person.from(prisoner)
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("prisonCodes")
  fun `should map prison nationality codes to cpr nationality codes`(prisonCode: String, cprCode: NationalityCode) {
    val prisoner = Prisoner(prisonNumber = randomPrisonNumber(), firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate(), nationality = prisonCode)
    val person = Person.from(prisoner)
    assertThat(person.nationalities.first().code).isEqualTo(cprCode)
  }

  @Test
  fun `should handle null common platform nationality code`() {
    val defendant = Defendant(id = randomDefendantId(), personDefendant = PersonDefendant(PersonDetails(lastName = randomName(), nationalityCode = null)))
    val person = Person.from(defendant)
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("commonPlatformCodes")
  fun `should map common platform nationality codes to cpr nationality codes`(commonPlatformCode: String, cprCode: NationalityCode) {
    val defendant = Defendant(id = randomDefendantId(), personDefendant = PersonDefendant(PersonDetails(lastName = randomName(), nationalityCode = commonPlatformCode)))
    val person = Person.from(defendant)
    assertThat(person.nationalities.first().code).isEqualTo(cprCode)
  }

  @Test
  fun `should handle null libra nationality code`() {
    val libraHearingEvent = LibraHearingEvent(cId = randomCId(), nationality1 = null, nationality2 = null)
    val person = Person.from(libraHearingEvent)
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("libraCodes")
  fun `should map libra nationality codes to cpr nationality codes`(libraCode: String, cprCode: NationalityCode) {
    val libraHearingEvent = LibraHearingEvent(cId = randomCId(), nationality1 = libraCode, nationality2 = libraCode)
    val person = Person.from(libraHearingEvent)
    assertThat(person.nationalities.size).isEqualTo(2)
    assertThat(person.nationalities.first().code).isEqualTo(cprCode)
  }

  companion object {

    @JvmStatic
    fun probationCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("ALBA", ALBA),
      Arguments.of("ALGE", ALGE),
      Arguments.of("ASM", ASM),
      Arguments.of("ANDO", ANDO),
      Arguments.of("ANGOL", ANGOL),
      Arguments.of("AG", AG),
      Arguments.of("ANTIG", ANTIG),
      Arguments.of("ARGEN", ARGEN),
      Arguments.of("UNKNOWN", UNKNOWN),
      Arguments.of("INVALID NATIONALITY CODE", UNKNOWN),
    )

    @JvmStatic
    fun prisonCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", ALBA),
      Arguments.of("Algerian", ALGE),
      Arguments.of("American Samoan", ASM),
      Arguments.of("Andorran", ANDO),
      Arguments.of("Angolan", ANGOL),
      Arguments.of("Anguillan", AG),
      Arguments.of("Citizen of Antigua and Barbuda", ANTIG),
      Arguments.of("Argentine", ARGEN),
      Arguments.of("UNKNOWN", UNKNOWN),
      Arguments.of("INVALID NATIONALITY CODE", UNKNOWN),
    )

    @JvmStatic
    fun commonPlatformCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("ALB", ALBA),
      Arguments.of("DZA", ALGE),
      Arguments.of("AND", ANDO),
      Arguments.of("AGO", ANGOL),
      Arguments.of("ATG", ANTIG),
      Arguments.of("ARG", ARGEN),
      Arguments.of("UNKNOWN", UNKNOWN),
      Arguments.of("INVALID NATIONALITY CODE", UNKNOWN),
    )

    @JvmStatic
    fun libraCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", ALBA, "Albanian"),
      Arguments.of("Algerian", ALGE, "Algerian"),
      Arguments.of("Andorran", ANDO, "Andorran"),
      Arguments.of("Angolan", ANGOL, "Angolan"),
      Arguments.of("Citizen of Antigua and Barbuda", ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("Argentinian", ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", UNKNOWN, "Unknown"),
    )
  }
}
