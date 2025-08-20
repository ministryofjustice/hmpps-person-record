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
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.stream.Stream

class NationalityCodeIntTest : IntegrationTestBase() {

  @Test
  fun `should handle null probation nationality code`() {
    val probationCase = ProbationCase(name = ProbationCaseName(firstName = randomName()), identifiers = Identifiers(crn = randomCrn()), nationality = Value(null))
    val person = createPerson(Person.from(probationCase))
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("probationCodes")
  fun `should map probation nationality codes to cpr nationality codes`(probationCode: String, cprCode: NationalityCode, cprDescription: String) {
    val probationCase = ProbationCase(name = ProbationCaseName(firstName = randomName()), identifiers = Identifiers(crn = randomCrn()), nationality = Value(probationCode))
    val person = createPerson(Person.from(probationCase))
    assertThat(person.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
    assertThat(person.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
  }

  @Test
  fun `handle null probation nationality code`() {
    val prisoner = Prisoner(prisonNumber = randomPrisonNumber(), firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate(), nationality = null)
    val person = createPerson(Person.from(prisoner))
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("prisonCodes")
  fun `should map prison nationality codes to cpr nationality codes`(prisonCode: String, cprCode: NationalityCode, cprDescription: String) {
    val prisoner = Prisoner(prisonNumber = randomPrisonNumber(), firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate(), nationality = prisonCode)
    val person = createPerson(Person.from(prisoner))
    assertThat(person.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
    assertThat(person.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
  }

  @Test
  fun `should handle null common platform nationality code`() {
    val defendant = Defendant(id = randomDefendantId(), personDefendant = PersonDefendant(PersonDetails(lastName = randomName(), nationalityCode = null)))
    val person = createPerson(Person.from(defendant))
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("commonPlatformCodes")
  fun `should map common platform nationality codes to cpr nationality codes`(commonPlatformCode: String, cprCode: NationalityCode, cprDescription: String) {
    val defendant = Defendant(id = randomDefendantId(), personDefendant = PersonDefendant(PersonDetails(lastName = randomName(), nationalityCode = commonPlatformCode)))
    val person = createPerson(Person.from(defendant))
    assertThat(person.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
    assertThat(person.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
  }

  @Test
  fun `should handle null libra nationality code`() {
    val libraHearingEvent = LibraHearingEvent(cId = randomCId(), nationality1 = null, nationality2 = null)
    val person = createPerson(Person.from(libraHearingEvent))
    assertThat(person.nationalities.size).isEqualTo(0)
  }

  @ParameterizedTest
  @MethodSource("libraCodes")
  fun `should map libra nationality codes to cpr nationality codes`(libraCode: String, cprCode: NationalityCode, cprDescription: String) {
    val libraHearingEvent = LibraHearingEvent(cId = randomCId(), nationality1 = libraCode, nationality2 = libraCode)
    val person = createPerson(Person.from(libraHearingEvent))
    assertThat(person.nationalities.size).isEqualTo(2)
    assertThat(person.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
    assertThat(person.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
  }

  companion object {

    @JvmStatic
    fun probationCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("ALBA", NationalityCode.ALBA, "Albanian"),
      Arguments.of("ALGE", NationalityCode.ALGE, "Algerian"),
      Arguments.of("ASM", NationalityCode.ASM, "American Samoan"),
      Arguments.of("ANDO", NationalityCode.ANDO, "Andorran"),
      Arguments.of("ANGOL", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("AG", NationalityCode.AG, "Anguillan"),
      Arguments.of("ANTIG", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("ARGEN", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun prisonCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", NationalityCode.ALBA, "Albanian"),
      Arguments.of("Algerian", NationalityCode.ALGE, "Algerian"),
      Arguments.of("American Samoan", NationalityCode.ASM, "American Samoan"),
      Arguments.of("Andorran", NationalityCode.ANDO, "Andorran"),
      Arguments.of("Angolan", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("Anguillan", NationalityCode.AG, "Anguillan"),
      Arguments.of("Citizen of Antigua and Barbuda", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("Argentine", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun commonPlatformCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("AL", NationalityCode.ALBA, "Albanian"),
      Arguments.of("DZ", NationalityCode.ALGE, "Algerian"),
      Arguments.of("AD", NationalityCode.ANDO, "Andorran"),
      Arguments.of("AO", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("AG", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("AR", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun libraCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", NationalityCode.ALBA, "Albanian"),
      Arguments.of("Algerian", NationalityCode.ALGE, "Algerian"),
      Arguments.of("Andorran", NationalityCode.ANDO, "Andorran"),
      Arguments.of("Angolan", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("Citizen of Antigua and Barbuda", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("Argentinian", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )
  }
}
