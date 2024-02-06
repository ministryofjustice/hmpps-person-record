package uk.gov.justice.digital.hmpps.personrecord.validate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.text.Charsets.UTF_8

class PNCIdValidatorTest {

  private var pncIdValidator: PNCIdValidator = PNCIdValidator()

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

  @ParameterizedTest
  @ValueSource(strings = ["20030011985Z", "20120052494O", "20230583843N", "2001/0171310S"])
  fun `should return invalid when PNC id is correctly formatted but not valid`(pncId: String) {
    // When
    val valid = pncIdValidator.isValid(PNCIdentifier(pncId))

    // Then
    assertThat(valid).isFalse()
  }

  @Test
  fun `validating PNCs from file`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), UTF_8)

    readAllLines.stream().forEach {
      assertThat(pncIdValidator.isValid(PNCIdentifier(it))).isTrue()
    }
  }
}
