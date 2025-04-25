package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import java.util.stream.Stream
import kotlin.test.assertEquals

class SexCodeTest {

  @ParameterizedTest
  @MethodSource("commonPlatformValues")
  fun `should map common platform gender values`(gender: String?, sexCode: SexCode?) {
    val personDetails = PersonDetails(
      gender = gender,
      lastName = "",
    )

    assertEquals(SexCode.from(personDetails), sexCode)
  }

  @ParameterizedTest
  @MethodSource("libraEventMessage")
  fun `should map libra event defendantSex values`(defendantSex: String?, sexCode: SexCode?) {
    val libraHearingEvent = LibraHearingEvent(defendantSex = defendantSex)

    assertEquals(SexCode.from(libraHearingEvent), sexCode)
  }

  @ParameterizedTest
  @MethodSource("probationNdeliusEvent")
  fun `should map from nDelius probation event gender values`(probationEvent: String?) {
  }

  companion object {
    @JvmStatic
    fun probationNdeliusEvent(): Stream<Arguments> = Stream.of(
      Arguments.of("M", SexCode.M),
      Arguments.of("F", SexCode.F),
      Arguments.of("N", SexCode.M),
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
  }
}
