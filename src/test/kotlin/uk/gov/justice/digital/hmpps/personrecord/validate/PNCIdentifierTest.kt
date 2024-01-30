package uk.gov.justice.digital.hmpps.personrecord.validate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PNCIdentifierTest {

  @Test
  fun `should process an empty string`() {
    // When
    val expectedCanonicalForm = PNCIdentifier("").pncId

    // Then
    assertThat(expectedCanonicalForm).isEmpty()
  }

  @Test
  fun `should process a null string`() {
    // When
    val expectedCanonicalForm = PNCIdentifier(null).pncId

    // Then
    assertThat(expectedCanonicalForm).isNull()
  }

  @ParameterizedTest
  @MethodSource("longFormPncProvider")
  fun `should convert long form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("canonicalFormPncProvider")
  fun `should NOT convert PNCs already in canonical form`(pncId: String, expectedResult: String) {
    // When
    val result = PNCIdentifier(pncId).pncId

    // Then
    assertThat(result).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("shortFormPncProvider")
  fun `should convert short form PNC ids to canonical form`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("invalidPncProvider")
  fun `should NOT convert invalid PNC ids`(pncId: String, expectedResult: String) {
    // When
    val expectedCanonicalForm = PNCIdentifier(pncId).pncId

    // Then
    assertThat(expectedCanonicalForm).isEqualTo(expectedResult)
  }

  @ParameterizedTest
  @MethodSource("equalityPncProvider")
  fun `should perform equality comparison using canonical form`(pncId1: String, pncId2: String, expectedResult: Boolean) {
    // When
    val result = PNCIdentifier(pncId1) == PNCIdentifier(pncId2)

    // Then
    assertThat(result).isEqualTo(expectedResult)
  }

  companion object {
    @JvmStatic
    fun shortFormPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("02/9Z", "2002/0000009Z"),
        Arguments.of("02/73319Z", "2002/0073319Z"),
        Arguments.of("00/73319Z", "2000/0073319Z"),
        Arguments.of("79/163001B", "1979/0163001B"),
        Arguments.of("0273319Z", "2002/0073319Z"),
        Arguments.of("0073319Z", "2000/0073319Z"),
        Arguments.of("79163001B", "1979/0163001B"),
        Arguments.of("840B", "1984/0000000B"),
      )
    }

    @JvmStatic
    fun longFormPncProvider(): Stream<Arguments> {
      return Stream.of(
        Arguments.of("20020073319Z", "2002/0073319Z"),
        Arguments.of("20000073319Z", "2000/0073319Z"),
        Arguments.of("19790163001B", "1979/0163001B"),
        Arguments.of("2002/0073319Z", "2002/0073319Z"),
        Arguments.of("2000/0073319Z", "2000/0073319Z"),
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
        Arguments.of("2000/0073319Z", "2000/0073319Z"),
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
