package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
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
  fun `should return person record for known provided surname`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Mortimer")

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].personId).isEqualTo(UUID.fromString("ddf11834-e2a3-11ed-b5ea-0242ac120002"))
  }

  @Test
  fun `should return person record for known provided middle names`() {
    // Given
    val searchRequest = PersonSearchRequest(
      surname = "Mahoney",
      forenameThree = "bob"
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].personId).isEqualTo(UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"))
  }

  @Test
  fun `should return all person records for provided search criteria`() {
    // Given
    val searchRequest = PersonSearchRequest(
      surname = "Mahoney"
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(2)
    assertThat(results).anyMatch { p ->
      p.personId == UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002") ||
      p.personId == UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002")
    }
  }


  @Test
  fun `should return a single person record for an exact name match ignoring case`() {
    // Given
    val searchRequest = PersonSearchRequest(
      forenameOne = "jOhN",
      surname = "mAhOnEy",
    )

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
  }

  @Test
  fun `should return an empty list for no matched request parameters`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Unknown")

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).isEmpty()
  }
}
