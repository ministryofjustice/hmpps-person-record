package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra.Name
import java.time.LocalDate

internal class PersonTest {

  @Test
  fun `should map libra hearing to person`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val libraHearingEvent = LibraHearingEvent(
      pnc = "1979/0026538X",
      name = Name(forename1 = "Stephen", surname = "King"),
      defendantDob = dateOfBirth,
    )

    // When
    val person = Person.from(libraHearingEvent)

    // Then
    assertThat(person.otherIdentifiers?.pncIdentifier).isEqualTo(PNCIdentifier.from("1979/0026538X"))
    assertThat(person.givenName).isEqualTo("Stephen")
    assertThat(person.familyName).isEqualTo("King")
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
  }

  @Test
  fun `should map common platform defendant to person`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val defendant = Defendant(
      pncId = "1979/0026538X",
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
    assertThat(person.otherIdentifiers?.pncIdentifier).isEqualTo(PNCIdentifier.from("1979/0026538X"))
    assertThat(person.givenName).isEqualTo("Stephen")
    assertThat(person.familyName).isEqualTo("King")
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
  }
}
