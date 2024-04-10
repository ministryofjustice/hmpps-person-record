package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

  @Test
  fun `should process a standard format CRO`() {
    val identifier = CROIdentifier.from("056810/65Y")
    assertEquals("056810/65Y", identifier.croId)
    assertEquals(true, identifier.fingerprint)
  }
}
