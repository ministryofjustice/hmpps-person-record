package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class CROIdentifierTest {
  @Test
  fun `should process an empty string`() {
    val identifier = CROIdentifier.from("")
    assertThat(identifier.croId.isEmpty())
  }

  @Test
  fun `should process null id`() {
    val identifier = CROIdentifier.from(null)
    assertThat(identifier.croId.isEmpty())
  }

  @Test
  fun `should process invalid id and stores it`() {
    val identifier = CROIdentifier.from("85227/65G")
    assertThat(identifier.croId.isEmpty())
    assertThat(identifier.valid).isFalse()
    assertThat(identifier.inputCro).isEqualTo("85227/65G")
  }

  @Test
  fun `should process a SF format CRO`() {
    val identifier = CROIdentifier.from("SF05/482703J")
    assertEquals("SF05/482703J", identifier.croId)
  }

  @Test
  fun `should process a SF format CRO with limit serial section should not pad serial num`() {
    val identifier = CROIdentifier.from("SF83/50058Z")
    assertEquals("SF83/50058Z", identifier.croId)
  }

  @Test
  fun `should process a standard format CRO`() {
    val identifier = CROIdentifier.from("265416/21G")
    assertEquals("265416/21G", identifier.croId)
  }

  @Test
  fun `should process a standard format CRO with limit serial section`() {
    val identifier = CROIdentifier.from("65656/91H")
    assertEquals("065656/91H", identifier.croId)
  }

  @Test
  fun `should process CROs`() {
    val readAllLines = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), Charsets.UTF_8)

    readAllLines.stream().forEach {
      assertThat(CROIdentifier.from(it).valid).isTrue().withFailMessage(it)
    }
  }
}
