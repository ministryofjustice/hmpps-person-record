package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.InvalidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.test.assertEquals

class CROIdentifierTest {
  @Test
  fun `should process an empty string`() {
    val identifier = CROIdentifier.from("")
    assertThat(identifier.croId.isEmpty())
    assertEquals(identifier.fingerprint, false)
  }

  @Test
  fun `should process null id`() {
    val identifier = CROIdentifier.from(null)
    assertThat(identifier.croId.isEmpty())
    assertEquals(identifier.fingerprint, false)
  }

  @Test
  fun `should process a SF format CRO`() {
    val identifier = CROIdentifier.from("SF20/01234523")
    assertThat(identifier.croId.isEmpty())
    assertEquals(identifier.fingerprint, false)
  }
}
