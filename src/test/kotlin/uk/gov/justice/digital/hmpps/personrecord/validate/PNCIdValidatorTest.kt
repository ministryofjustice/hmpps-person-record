package uk.gov.justice.digital.hmpps.personrecord.validate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService

@ExtendWith(MockitoExtension::class)
class PNCIdValidatorTest {

  @Mock
  lateinit var telemetryService: TelemetryService

  private lateinit var pncIdValidator: PNCIdValidator

  @BeforeEach
  fun setUp() {
    pncIdValidator = PNCIdValidator(telemetryService)
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["01", "012", "0123", "01234", "012345", "0123567", "012345678", "0123456789", "01234567890", "01234567890123"],
  )
  fun `should return invalid when PNC id is not the correct length`(pncId: String) {
    // When
    val valid = pncIdValidator.isValid(PNCIdentifier(pncId))

    // Then
    assertThat(valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["1X23/1234567A", "1923[1234567A", "1923/1Z34567A", "1923/1234567AA"])
  fun `should return invalid when PNC id is incorrectly formatted`(pncId: String) {
    // When
    val valid = pncIdValidator.isValid(PNCIdentifier(pncId))

    // Then
    assertThat(valid).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["20030011985X", "20120052494Q", "20230583843L", "2001/0171310W", "2011/0275516Q", "2008/0056560Z", "2003/0062845E", "1981/0154257C"])
  fun `should return valid when PNC id is correctly formatted`(pncId: String) {
    // When
    val valid = pncIdValidator.isValid(PNCIdentifier(pncId))

    // Then
    assertThat(valid).isTrue()
  }

  @Test
  fun `should return Z check digit for an input of 0`() {
    // Given
    val index = 0

    // When
    val letterInAlphabet = PNCIdValidator.convertNumberToLetterInAlphabet(index)

    // Then
    assertThat(letterInAlphabet).isEqualTo('Z')
  }

  @Test
  fun `should return A check digit for an input of 1`() {
    // Given
    val index = 1

    // When
    val letterInAlphabet = PNCIdValidator.convertNumberToLetterInAlphabet(index)

    // Then
    assertThat(letterInAlphabet).isEqualTo('A')
  }

  @Test
  fun `should return Y check digit for an input of 22`() {
    // Given
    val index = 22

    // When
    val letterInAlphabet = PNCIdValidator.convertNumberToLetterInAlphabet(index)

    // Then
    assertThat(letterInAlphabet).isEqualTo('Y')
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22])
  fun `should not return ignored letters for any input`(input: Int) {
    // Given
    val index = 22

    // When
    val letterInAlphabet = PNCIdValidator.convertNumberToLetterInAlphabet(index)

    // Then
    assertThat(letterInAlphabet).isNotIn('I', 'O', 'S')
  }

  @Test
  fun `should throw exception for inputs outside of number range`() {
    // when
    val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
      PNCIdValidator.convertNumberToLetterInAlphabet(99)
    }

    // Then
    assertThat(exception.message).contains("Number: 99 is out of range")
  }
}
