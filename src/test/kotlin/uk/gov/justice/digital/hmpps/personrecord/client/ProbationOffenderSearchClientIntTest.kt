package uk.gov.justice.digital.hmpps.personrecord.client

import feign.FeignException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase

class ProbationOffenderSearchClientIntTest : IntegrationTestBase() {

  @Autowired
//  lateinit var restClient: ProbationOffenderSearchClient
  lateinit var restClient: ProbationOffenderSearchClient

  @Test
  fun `should return offender details for known person`() {
    // Given
    val searchDto = SearchDto(firstName = "John", surname = "Smith")

    // When
    val offenderDetails = restClient.getOffenderDetail(searchDto)

    // Then
    assertThat(offenderDetails).hasSize(7)
      .extracting("otherIds.crn")
      .contains("X026350", "X335837", "X335837", "X335837", "X108153", "X181760", "X181789")
  }

  @Test
  fun `should return empty list for unknown person details`() {
    // Given
    val searchDto = SearchDto(crn = "CRN404")

    // When
    val offenderDetails = restClient.getOffenderDetail(searchDto)

    // Then
    assertThat(offenderDetails).isEmpty()
  }

  @Test
  fun `should return bad request for insufficient search parameters`() {
    // Given
    val searchDto = SearchDto()

    // When
    val exception = assertThrows(FeignException.BadRequest::class.java) {
      restClient.getOffenderDetail(searchDto)
    }

    // Then
    assertThat(exception.message).contains("[400 Bad Request] during [GET]", "Invalid search  - please provide at least 1 search parameter")
  }
}
