package uk.gov.justice.digital.hmpps.personrecord.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PossibleMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.time.LocalDate

class PrisonerSearchClientIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var restClient: PrisonerSearchClient

  @Test
  fun `should return prisoner details for given match criteria`() {
    // Given
    val dob = LocalDate.of(1975, 4, 2)
    val possibleMatchCriteria = PossibleMatchCriteria(
      firstName = "Robert",
      lastName = "Larsen",
      dateOfBirth = dob
    )

    // When
    val prisoners = restClient.findPossibleMatches(possibleMatchCriteria)

    // Then
    assertThat(prisoners).isNotEmpty.hasSize(1)
    assertThat(prisoners?.get(0)?.prisonerNumber).isEqualTo("A1234AA")
    assertThat(prisoners?.get(0)?.pncNumber).isEqualTo("12/394773H")
    assertThat(prisoners?.get(0)?.gender).isEqualTo("Female")
    assertThat(prisoners?.get(0)?.nationality).isEqualTo("Egyptian")
  }

  @Test
  fun `should return empty prisoner list for unmatched search criteria`() {
    // Given
    val possibleMatchCriteria = PossibleMatchCriteria(
      firstName = "Melanie",
      lastName = "Sykes"
    )

    // When
    val prisoners = restClient.findPossibleMatches(possibleMatchCriteria)

    // Then
    assertThat(prisoners).isEmpty()
  }
}
