package uk.gov.justice.digital.hmpps.personrecord.client

import feign.FeignException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase

class ProbationOffenderSearchClientIntTest : WebTestBase() {

  @Autowired
  lateinit var restClient: ProbationOffenderSearchClient

  @Test
  fun `should return offender details for known person`() {
    // Given
    val offenderMatchCriteria = OffenderMatchCriteria(firstName = "John", surname = "Smith")

    // When
    val offenderDetails = restClient.findPossibleMatches(offenderMatchCriteria)

    // Then
    assertThat(offenderDetails).hasSize(7)
      .extracting("otherIds.crn")
      .contains("X026350", "X335837", "X335837", "X335837", "X108153", "X181760", "X181789")
  }

  @Test
  fun `should return empty list for unknown person details`() {
    // Given
    val offenderMatchCriteria = OffenderMatchCriteria(crn = "CRN404")

    // When
    val offenderDetails = restClient.findPossibleMatches(offenderMatchCriteria)

    // Then
    assertThat(offenderDetails).isEmpty()
  }

  @Test
  fun `should return bad request for insufficient search parameters`() {
    // Given
    val offenderMatchCriteria = OffenderMatchCriteria(includeAliases = null)

    // When
    val exception = assertThrows(FeignException.BadRequest::class.java) {
      restClient.findPossibleMatches(offenderMatchCriteria)
    }

    // Then
    assertThat(exception.message).contains("[400 Bad Request] during [GET]", "Invalid search  - please provide at least 1 search parameter")
  }
}
