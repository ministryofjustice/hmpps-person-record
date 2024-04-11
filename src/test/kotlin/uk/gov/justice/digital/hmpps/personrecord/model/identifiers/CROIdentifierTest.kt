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
    val identifier = CROIdentifier.from("SF05/482703J")
    assertEquals("482703/05J", identifier.croId)
    assertEquals(false, identifier.fingerprint)
  }

  @Test
  fun `should process a SF format CRO with limit serial section`() {
    val identifier = CROIdentifier.from("SF83/50058Z")
    assertEquals("050058/83Z", identifier.croId)
    assertEquals(false, identifier.fingerprint)
  }

  @Test
  fun `should process a standard format CRO`() {
    val identifier = CROIdentifier.from("265416/21G")
    assertEquals("265416/21G", identifier.croId)
    assertEquals(true, identifier.fingerprint)
  }

  @Test
  fun `should process a standard format CRO with limit serial section`() {
    val identifier = CROIdentifier.from("65656/91H")
    assertEquals("065656/91H", identifier.croId)
    assertEquals(true, identifier.fingerprint)
  }

  @Test
  fun `should process CROs`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), Charsets.UTF_8)

    readAllLines.stream().forEach {
      println(it)
      assertThat(CROIdentifier.from(it).valid).isTrue().withFailMessage(it)
    }
  }
}
