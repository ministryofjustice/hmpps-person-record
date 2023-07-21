package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.*

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class PersonRepositoryCustomImplIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepositoryCustomImpl

  @Test
  @Disabled("Until refactoring complete")
  fun `should return person records with minimum search criteria provided`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Mahoney")

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].personId).isEqualTo(UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"))
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return person records with all search criteria provided`() {
    // Given
    val searchRequest = PersonSearchRequest(
      pncNumber = "PNC12345",
      forename = "Carey",
      middleNames = listOf("Iestyn"),
      surname = "Mahoney",
      dateOfBirth = LocalDate.of(1965, 6, 18),
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].personId).isEqualTo(UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"))
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return person records for null middle names in search request`() {
    // Given
    val searchRequest = PersonSearchRequest(
      middleNames = null,
      surname = "Evans",
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results)
      .hasSize(3)
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return a single person record for an exact name match ignoring case`() {
    // Given
    val searchRequest = PersonSearchRequest(
      forename = "GeRwIn",
      middleNames = listOf("DAFYDD", "jenkins"),
      surname = "evanS",
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return an empty list for no matched request parameters`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Unknown")

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).isEmpty()
  }
}
