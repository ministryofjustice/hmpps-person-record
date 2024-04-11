package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Paths

import kotlin.test.assertEquals

class CROIdentifierTest {
  @Test
  fun `should process an empty string`() {
    val identifier = CROIdentifier.from("")
    assertThat(identifier.croId.isEmpty())
    assertEquals(false, identifier.fingerprint)
  }

  @Test
  fun `should process null id`() {
    val identifier = CROIdentifier.from(null)
    assertThat(identifier.croId.isEmpty())
    assertEquals(false, identifier.fingerprint)
  }

  @Test
  fun `should process a SF format CRO`() {
    val identifier = CROIdentifier.from("SF20/012345A")
    assertEquals("012345/20A", identifier.croId)
    assertEquals(false, identifier.fingerprint)
  }

  @Test
  fun `should process a SF format CRO with limit serial section`() {
    val identifier = CROIdentifier.from("SF20/0145A")
    assertEquals("000145/20A", identifier.croId)
    assertEquals(false, identifier.fingerprint)
  }

  @ParameterizedTest
  @ValueSource(
    strings = ["265416/21G"]
  )
  fun `should process a standard format CRO`(croId: String) {
    val identifier = CROIdentifier.from(croId)
    assertEquals(croId, identifier.croId)
    assertEquals(true, identifier.fingerprint)
  }

  @Test
  fun `should process CROs`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), Charsets.UTF_8)

    readAllLines.stream().forEach {
      assertThat(CROIdentifier.from(it).valid)
    }
  }
}
