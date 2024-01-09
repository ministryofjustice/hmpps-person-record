package uk.gov.justice.digital.hmpps.personrecord.client

import feign.FeignException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.time.LocalDate

class OffenderDetailRestClientIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var restClient: OffenderDetailRestClient

  @Test
  fun `should return delius offender detail for valid CRN`() {
    // Given
    val crn = "C1234"

    // When
    val deliusOffenderDetail = restClient.getNewOffenderDetail("/probation-case.engagement.created/$crn")

    // Then
    assertThat(deliusOffenderDetail?.identifiers?.crn).isEqualTo(crn)
    assertThat(deliusOffenderDetail?.dateOfBirth).isEqualTo(LocalDate.of(1939, 10, 10))
  }

  @Test
  fun `should throw forbidden exception when not authorised`() {
    // Given
    val crn = "AB12345"

    // When
    val exception = assertThrows(FeignException.Forbidden::class.java) {
      restClient.getNewOffenderDetail("/probation-case.engagement.created/$crn")
    }

    // Then
    assertThat(exception.message).containsAnyOf("You are excluded from viewing this offender record. Please contact a system administrator")
  }
}
