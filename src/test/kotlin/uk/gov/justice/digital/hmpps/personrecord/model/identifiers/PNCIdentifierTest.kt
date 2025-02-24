package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

class PNCIdentifierTest {

  @Test
  fun `should process an empty string`() {
    assertThat(PNCIdentifier.from("").valid).isFalse()
  }

  @Test
  fun `should process a null string`() {
    assertThat(PNCIdentifier.from(null).valid).isFalse()
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
  @ValueSource(
    strings = ["01", "012", "0123", "01234", "012345", "0123567", "012345678", "0123456789", "01234567890", "01234567890123"],
  )
  fun `should return invalid when PNC id is not the correct length`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["TOTALLYINVALID", "1X23/1234567A", "1923[1234567A", "1923/1Z34567A", "1923/1234567AA"])
  fun `should return invalid when PNC id is incorrectly formatted`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["2008/0056560Z", "20030011985X", "20120052494Q", "20230583843L", "2001/0171310W", "2011/0275516Q", "2008/0056560Z", "2003/0062845E", "1981/0154257C"])
  fun `should return valid when PNC id is correctly formatted`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.valid).isTrue()
  }

  @ParameterizedTest
  @ValueSource(strings = ["20030011985Z", "20120052494O", "20230583843N", "2001/0171310S"])
  fun `should return invalid when PNC id is correctly formatted but not valid`(pncId: String) {
    val pncIdentifier = PNCIdentifier.from(pncId)
    assertThat(pncIdentifier.valid).isFalse()
  }

  @Test
  fun `validating PNCs from file`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), Charsets.UTF_8)

    readAllLines.stream().forEach {
      assertThat((PNCIdentifier.from(it).pncId)).isNotEmpty()
    }
  }

  @Test
  fun `invalid PNC is equal`() {
    assertThat(PNCIdentifier.from("invalid")).isEqualTo(PNCIdentifier.from("invalid"))
  }

  @Test
  fun `valid PNC is equal`() {
    assertThat(PNCIdentifier.from("1979/0163001B")).isEqualTo(PNCIdentifier.from("1979/0163001B"))
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
    )

    @JvmStatic
    fun longFormPncProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("20020073319Z", "2002/0073319Z"),
      Arguments.of("19790163001B", "1979/0163001B"),
      Arguments.of("2002/0073319Z", "2002/0073319Z"),
      Arguments.of("1979/0163001B", "1979/0163001B"),
    )

    @JvmStatic
    fun canonicalFormPncProvider(): Stream<Arguments> = Stream.of(
      Arguments.of("1979/0163001B", "1979/0163001B"),
      Arguments.of("2002/0073319Z", "2002/0073319Z"),
    )
  }
}
