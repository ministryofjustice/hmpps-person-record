package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra.Name
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
    assertThat(person.otherIdentifiers?.pncIdentifier).isEqualTo("353344/D")
    assertThat(person.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(person.givenName).isEqualTo("Steve")
    assertThat(person.familyName).isEqualTo("Jones")
    assertThat(person.middleNames).contains("Frankie")
  }

  @Test
  fun `should map libra hearing to person`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val libraHearingEvent = LibraHearingEvent(
      pnc = "PNC1234",
      name = Name(forename1 = "Stephen", surname = "King"),
      defendantDob = dateOfBirth,
    )

    // When
    val person = Person.from(libraHearingEvent)

    // Then
    assertThat(person.otherIdentifiers?.pncIdentifier?.pncId).isEqualTo("PNC1234")
    assertThat(person.givenName).isEqualTo("Stephen")
    assertThat(person.familyName).isEqualTo("King")
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
  }

  @Test
  fun `should map common platform defendant to person`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val defendant = Defendant(
      pncId = "PNC1234",
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          firstName = "Stephen",
          lastName = "King",
          dateOfBirth = dateOfBirth,
          gender = "M",
        ),
      ),
    )

    // When
    val person = Person.from(defendant)

    // Then
    assertThat(person.otherIdentifiers?.pncIdentifier?.pncId).isEqualTo("PNC1234")
    assertThat(person.givenName).isEqualTo("Stephen")
    assertThat(person.familyName).isEqualTo("King")
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
  }
}
