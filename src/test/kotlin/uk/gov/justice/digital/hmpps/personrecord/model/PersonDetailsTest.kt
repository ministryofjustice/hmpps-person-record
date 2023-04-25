package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

internal class PersonDetailsTest {

  @Test
  fun `should correctly map multiple middle names to a list`() {
    // Given
    val person = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
      middleNames = "Jack Michael Henry",
      familyName = "Jones",
    )

    // When
    val personDetails = PersonDetails.from(person)

    // Then
    assertThat(personDetails.middleNames).containsExactlyInAnyOrder("Jack", "Michael", "Henry")
  }

  @Test
  fun `should return an empty list when no middle names are present`() {
    // Given
    val person = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
      familyName = "Jones",
    )

    // When
    val personDetails = PersonDetails.from(person)

    // Then
    assertThat(personDetails.middleNames).isEmpty()
  }

  @Test
  fun `should correctly map all fields`() {
    // Given
    val date = LocalDate.now()
    val person = PersonEntity(
      id = 3234L,
      dateOfBirth = date,
      pncNumber = "353344/D",
      crn = "CRN1234",
      givenName = "Steve",
      familyName = "Jones",
      middleNames = "Frankie",
    )

    // When
    val personDetails = PersonDetails.from(person)

    // Then
    assertThat(personDetails.dateOfBirth).isEqualTo(date)
    assertThat(personDetails.otherIdentifiers?.pncNumber).isEqualTo("353344/D")
    assertThat(personDetails.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(personDetails.givenName).isEqualTo("Steve")
    assertThat(personDetails.familyName).isEqualTo("Jones")
    assertThat(personDetails.middleNames).contains("Frankie")
  }
}
