package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

internal class PersonTest {

  @Test
  @Disabled("Until refactoring complete")
  fun `should correctly map multiple middle names to a list`() {
    // Given
    val personEntity = PersonEntity(
      id = 3234L,
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.middleNames).containsExactlyInAnyOrder("Jack", "Michael", "Henry")
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should correctly map single middle name to a list`() {
    // Given
    val personEntity = PersonEntity(
      id = 3234L,
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.middleNames).containsExactlyInAnyOrder("Jack")
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return an empty list when no middle names are present`() {
    // Given
    val personEntity = PersonEntity(
      id = 3234L,
    )

    // When
    val person = Person.from(personEntity)

    // Then
    assertThat(person.middleNames).isEmpty()
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should correctly map all fields`() {
    // Given
    val date = LocalDate.now()
    val personEntity = PersonEntity(
      id = 3234L,
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
