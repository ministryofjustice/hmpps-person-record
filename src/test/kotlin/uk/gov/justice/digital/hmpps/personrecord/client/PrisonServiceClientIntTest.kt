package uk.gov.justice.digital.hmpps.personrecord.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.time.LocalDate

class PrisonServiceClientIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var restClient: PrisonServiceClient

  @Test
  fun `should return prisoner detail for prisoner number which exists`() {
    // Given
    val prisonerNumber = "A1234AA"

    // When
    val prisonerDetails = restClient.getPrisonerDetails(prisonerNumber)

    // Then
    assertThat(prisonerDetails?.offenderId).isEqualTo(356)
    assertThat(prisonerDetails?.rootOffenderId).isEqualTo(300)
    assertThat(prisonerDetails?.dateOfBirth).isEqualTo(LocalDate.of(1970, 3, 15))
  }
}
