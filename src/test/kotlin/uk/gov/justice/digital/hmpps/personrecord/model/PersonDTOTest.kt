package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

internal class PersonDTOTest {

  @BeforeEach
  fun setUp() {
  }

  @Test
  fun `should correctly map multiple middle names to a list`() {
    // Given
    val person = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
      middleNames = "Jack Michael Henry",
    )

    // When
    val personDTO = PersonDTO.from(person)

    // Then
    assertThat(personDTO.middleNames).containsExactlyInAnyOrder("Jack", "Michael", "Henry")
  }

  @Test
  fun `should return an empty list when no middle names are present`() {
    // Given
    val person = PersonEntity(
      id = 3234L,
      dateOfBirth = LocalDate.now(),
    )

    // When
    val personDTO = PersonDTO.from(person)

    // Then
    assertThat(personDTO.middleNames).isEmpty()
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
    val personDTO = PersonDTO.from(person)

    // Then
    assertThat(personDTO.dateOfBirth).isEqualTo(date)
    assertThat(personDTO.otherIdentifiers?.pncNumber).isEqualTo("353344/D")
    assertThat(personDTO.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(personDTO.givenName).isEqualTo("Steve")
    assertThat(personDTO.familyName).isEqualTo("Jones")
    assertThat(personDTO.middleNames).contains("Frankie")
  }
}
