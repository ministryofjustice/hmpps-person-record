package uk.gov.justice.digital.hmpps.personrecord.validate

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
    // When
    val expectedCanonicalForm = PNCIdentifier.create("").pncId

    // Then
    assertThat(expectedCanonicalForm).isEmpty()
  }

  @Test
  fun `should process a null string`() {
    // When
    val expectedCanonicalForm = PNCIdentifier.create(null).pncId

    // Then
    assertThat(expectedCanonicalForm).isEmpty()
  }

  @ParameterizedTest
  @MethodSource("longFormPncProvider")
  fun `should convert long form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier.create(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("canonicalFormPncProvider")
  fun `should NOT convert PNCs already in canonical form`(pncId: String, expectedResult: String) {
    // When
    val result = PNCIdentifier.create(pncId).pncId

    // Then
    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("shortFormPncProvider")
  fun `should convert short form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier.create(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("invalidPncProvider")
  fun `should NOT convert invalid PNC ids`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier.create(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult.uppercase())
  }

  @ParameterizedTest
  @MethodSource("equalityPncProvider")
  fun `should perform equality comparison using canonical form`(pncId1: String, pncId2: String, expectedResult: Boolean) {
    // When
    val result = PNCIdentifier.create(pncId1) == PNCIdentifier.create(pncId2)

    // Then
    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["01", "012", "0123", "01234", "012345", "0123567", "012345678", "0123456789", "01234567890", "01234567890123"],
  )
  fun `should return invalid when PNC id is not the correct length`(pncId: String) {
    // When
    val valid = PNCIdentifier.create(pncId).isValid()

    // Then
    assertThat(valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["1X23/1234567A", "1923[1234567A", "1923/1Z34567A", "1923/1234567AA"])
  fun `should return invalid when PNC id is incorrectly formatted`(pncId: String) {
    // When
    val valid = PNCIdentifier.create(pncId).isValid()

    // Then
    assertThat(valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["20030011985X", "20120052494Q", "20230583843L", "2001/0171310W", "2011/0275516Q", "2008/0056560Z", "2003/0062845E", "1981/0154257C"])
  fun `should return valid when PNC id is correctly formatted`(pncId: String) {
    // When
    val valid = PNCIdentifier.create(pncId).isValid()

    // Then
    assertThat(valid).isTrue()
  }

  @ParameterizedTest
  @ValueSource(strings = ["20030011985Z", "20120052494O", "20230583843N", "2001/0171310S"])
  fun `should return invalid when PNC id is correctly formatted but not valid`(pncId: String) {
    // When
    val valid = PNCIdentifier.create(pncId).isValid()

    // Then
    assertThat(valid).isFalse()
  }

  @Test
  fun `validating PNCs from file`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), Charsets.UTF_8)

    readAllLines.stream().forEach {
      assertThat((PNCIdentifier.create(it).isValid())).isTrue()
    }
  }

  companion object {
    @JvmStatic
    fun shortFormPncProvider(): Stream<Arguments> {
      return Stream.of(

        Arguments.of("81/34U", "1981/0000034U"),
        Arguments.of("02/73319Z", "2002/0073319Z"),
        Arguments.of("00/223R", "2000/0000223R"),
        Arguments.of("79/163001B", "1979/0163001B"),
        Arguments.of("0273319Z", "2002/0073319Z"),
        Arguments.of("79163001B", "1979/0163001B"),
        Arguments.of("033Y", "2003/0000003Y"),
      )
    }

    @JvmStatic
    fun longFormPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("20020073319Z", "2002/0073319Z"),
        Arguments.of("19790163001B", "1979/0163001B"),
        Arguments.of("2002/0073319Z", "2002/0073319Z"),
        Arguments.of("1979/0163001B", "1979/0163001B"),
      )
    }

    @JvmStatic
    fun equalityPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("19790163001B", "79/163001B", true),
        Arguments.of("19790163001B", "1979/0163001B", true),
        Arguments.of("1979/0163001B", "1979/0163001B", true),
        Arguments.of("1979/0163001B", "1980/0163001B", false),
        Arguments.of("02/73319Z", "20020073319Z", true),
      )
    }

    @JvmStatic
    fun canonicalFormPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("1979/0163001B", "1979/0163001B"),
        Arguments.of("2002/0073319Z", "2002/0073319Z"),
      )
    }

    @JvmStatic
    fun invalidPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("garbage", "garbage"),
        Arguments.of("xx/123456Z", "xx/123456Z"),
        Arguments.of("sdsdlkfjlsdkfjlskdjflsdkfj", "sdsd/lkfjlsdkfjlskdjflsdkfj"),
      )
    }
  }
}
