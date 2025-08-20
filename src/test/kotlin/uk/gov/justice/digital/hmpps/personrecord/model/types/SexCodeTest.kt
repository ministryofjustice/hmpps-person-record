package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import java.util.stream.Stream

class SexCodeTest {

  @ParameterizedTest
  @MethodSource("commonPlatformValues")
  fun `should map common platform gender values`(gender: String?, sexCode: SexCode?) {
    val personDetails = PersonDetails(
      gender = gender,
      lastName = "",
    )

    assertThat(SexCode.from(personDetails)).isEqualTo(sexCode)
  }

  @ParameterizedTest
  @MethodSource("libraEventMessage")
  fun `should map libra event defendantSex values`(defendantSex: String?, sexCode: SexCode?) {
    val libraHearingEvent = LibraHearingEvent(defendantSex = defendantSex)

    assertThat(SexCode.from(libraHearingEvent)).isEqualTo(sexCode)
  }

  @ParameterizedTest
  @MethodSource("probationEvent")
  fun `should map from probation event gender values`(genderCode: String?, sexCode: SexCode?) {
    val probationCase = ProbationCase(name = ProbationCaseName(), identifiers = Identifiers(), gender = Value(genderCode))

    assertThat(SexCode.from(probationCase)).isEqualTo(sexCode)
  }

  @ParameterizedTest
  @MethodSource("prisonEvent")
  fun `should map from prison event gender values`(gender: String?, sexCode: SexCode?) {
    val probationCase = Prisoner(
      prisonNumber = "", gender = gender,
      title = "",
      firstName = "",
      middleNames = "",
      lastName = "",
      nationality = "",
      religion = "",
      ethnicity = "",
      pnc = null,
      cro = null,
      dateOfBirth = randomDate(),
    )

    assertThat(SexCode.from(probationCase)).isEqualTo(sexCode)
  }

  companion object {
    @JvmStatic
    fun probationEvent(): Stream<Arguments> = Stream.of(
      Arguments.of("M", SexCode.M),
      Arguments.of("F", SexCode.F),
      Arguments.of("N", SexCode.N),
      Arguments.of("ANYTHING ELSE", SexCode.NS),
      Arguments.of(null, null),
    )

    @JvmStatic
    fun libraEventMessage(): Stream<Arguments> = Stream.of(
      Arguments.of("M", SexCode.M),
      Arguments.of("F", SexCode.F),
      Arguments.of("NS", SexCode.NS),
      Arguments.of("ANYTHING ELSE", SexCode.N),
      Arguments.of(null, null),
    )

    @JvmStatic
    fun commonPlatformValues(): Stream<Arguments> = Stream.of(
      Arguments.of("MALE", SexCode.M),
      Arguments.of("FEMALE", SexCode.F),
      Arguments.of("NOT SPECIFIED", SexCode.NS),
      Arguments.of("ANYTHING ELSE", SexCode.N),
      Arguments.of(null, null),
    )

    @JvmStatic
    fun prisonEvent(): Stream<Arguments> = Stream.of(
      Arguments.of("Male", SexCode.M),
      Arguments.of("Female", SexCode.F),
      Arguments.of("Not Known / Not Recorded", SexCode.N),
      Arguments.of("Not Specified (Indeterminate)", SexCode.NS),
      Arguments.of("ANYTHING ELSE", SexCode.N),
      Arguments.of(null, null),
    )
  }
}
