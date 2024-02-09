package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.util.*

class PersonRepositoryCustomImplIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setup() {
    personRepository.deleteAll()
  }

  @Test
  fun `should return person record for known provided surname`() {
    // Given
    aDefendant(UUID.fromString("ddf11834-e2a3-11ed-b5ea-0242ac120002"), "Bob", "Mortimer")

    val searchRequest = PersonSearchRequest(surname = "Mortimer")

    // When
    val results = personRepository.searchByRequestParameters(searchRequest)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].personId).isEqualTo(UUID.fromString("ddf11834-e2a3-11ed-b5ea-0242ac120002"))
  }

  @Test
  fun `should return all person records for provided search criteria`() {
    // Given
    aDefendant(personId = UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"), givenName = "Iestyn", familyName = "Mahoney")
    aDefendant(personId = UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002"), givenName = "Garry", familyName = "Mahoney")
    val searchRequest = PersonSearchRequest(
      surname = "Mahoney",
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
    aDefendant(givenName = "John", familyName = "Mahoney")
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
