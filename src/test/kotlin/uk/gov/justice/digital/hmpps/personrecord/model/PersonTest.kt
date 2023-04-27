package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

internal class PersonTest {

  @Test
  fun `should correctly map multiple middle names to a list`() {
    // Given
    val personEntity = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
      middleNames = "Jack Michael Henry",
      familyName = "Jones",
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.middleNames).containsExactlyInAnyOrder("Jack", "Michael", "Henry")
  }

  @Test
  fun `should return an empty list when no middle names are present`() {
    // Given
    val personEntity = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
      familyName = "Jones",
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.middleNames).isEmpty()
  }

  @Test
  fun `should correctly map all fields`() {
    // Given
    val date = LocalDate.now()
    val personEntity = PersonEntity(
      id = 3234L,
      dateOfBirth = date,
      pncNumber = "353344/D",
      crn = "CRN1234",
      givenName = "Steve",
      familyName = "Jones",
      middleNames = "Frankie",
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.dateOfBirth).isEqualTo(date)
    assertThat(person.otherIdentifiers?.pncNumber).isEqualTo("353344/D")
    assertThat(person.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(person.givenName).isEqualTo("Steve")
    assertThat(person.familyName).isEqualTo("Jones")
    assertThat(person.middleNames).contains("Frankie")
  }
}
