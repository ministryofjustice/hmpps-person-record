package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate
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
    aDefendant(UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"), "Iestyn", "Mahoney")
    aDefendant(UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002"), "Garry", "Mahoney")
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
    aDefendant(UUID.randomUUID(), "John", "Mahoney")
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

  private fun aDefendant(personId: UUID, givenName: String, familyName: String) {
    val person = Person(otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2001/0171310W"), crn = "CRN1234"), givenName = givenName, familyName = familyName, dateOfBirth = LocalDate.of(1965, 6, 18))

    val newPersonEntity = PersonEntity(personId = personId)
    newPersonEntity.createdBy = "test"
    newPersonEntity.lastUpdatedBy = "test"
    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)
    personRepository.saveAndFlush(newPersonEntity)
  }
}
