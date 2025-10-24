package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

class PNCIdentifierTest {

  @Test
  fun `should process an empty string`() {
    assertThat(PNCIdentifier.from("").pncId).isEqualTo("")
  }

  @Test
  fun `should process a null string`() {
    assertThat(PNCIdentifier.from(null).pncId).isEqualTo("")
  }

  @ParameterizedTest
  @MethodSource("longFormPncProvider")
  fun `should convert long form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    assertThat(PNCIdentifier.from(pncId).pncId).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("canonicalFormPncProvider")
  fun `should NOT convert PNCs already in canonical form`(pncId: String, expectedResult: String) {
    assertThat(PNCIdentifier.from(pncId).pncId).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("shortFormPncProvider")
  fun `should convert short form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    assertThat(PNCIdentifier.from(pncId).pncId).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("equalityPncProvider")
  fun `should perform equality comparison using canonical form`(pncId1: String, pncId2: String, expectedResult: Boolean) {
    val result = PNCIdentifier.from(pncId1) == PNCIdentifier.from(pncId2)
    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["01", "012", "0123", "01234", "012345", "0123567", "012345678", "0123456789", "01234567890", "01234567890123"],
  )
  fun `should return invalid when PNC id is not the correct length`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.pncId).isEqualTo("")
  }

  @ParameterizedTest
  @ValueSource(strings = ["TOTALLYINVALID", "1X23/1234567A", "1923[1234567A", "1923/1Z34567A", "1923/1234567AA"])
  fun `should return invalid when PNC id is incorrectly formatted`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.pncId).isEqualTo("")
  }

  @ParameterizedTest
  @ValueSource(strings = ["20030011985Z", "20120052494O", "20230583843N", "2001/0171310S"])
  fun `should return invalid when PNC id is correctly formatted but not valid`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.pncId).isEqualTo("")
  }

  companion object {
    @JvmStatic
    fun shortFormPncProvider(): Stream<Arguments> = Stream.of(

      Arguments.of("81/34U", "1981/0000034U"),
      Arguments.of("02/73319Z", "2002/0073319Z"),
      Arguments.of("00/223R", "2000/0000223R"),
      Arguments.of("79/163001B", "1979/0163001B"),
      Arguments.of("0273319Z", "2002/0073319Z"),
      Arguments.of("79163001B", "1979/0163001B"),
      Arguments.of("033Y", "2003/0000003Y"),
      Arguments.of("67/9893091D", "1967/9893091D"),
    )

    @JvmStatic
    fun longFormPncProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("20020073319Z", "2002/0073319Z"),
      Arguments.of("19790163001B", "1979/0163001B"),
      Arguments.of("2002/0073319Z", "2002/0073319Z"),
      Arguments.of("1979/0163001B", "1979/0163001B"),
    )

    @JvmStatic
    fun equalityPncProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("19790163001B", "79/163001B", true),
      Arguments.of("19790163001B", "1979/0163001B", true),
      Arguments.of("1979/0163001B", "1979/0163001B", true),
      Arguments.of("1979/0163001B", "1980/0163001B", false),
      Arguments.of("02/73319Z", "20020073319Z", true),
    )

    @JvmStatic
    fun canonicalFormPncProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("1979/0163001B", "1979/0163001B"),
      Arguments.of("2002/0073319Z", "2002/0073319Z"),
    )
  }
}
