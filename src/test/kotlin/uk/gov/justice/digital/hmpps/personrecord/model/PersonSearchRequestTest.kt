package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra.Name
import java.time.LocalDate

class PersonSearchRequestTest {

  @Test
  fun `should map libra hearing to person search request`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val libraHearingEvent = LibraHearingEvent(
      pnc = "PNC1234",
      name = Name(forename1 = "Stephen", surname = "King"),
      defendantDob = dateOfBirth,
    )

    // When
    val personSearchRequest = PersonSearchRequest.from(libraHearingEvent)

    // Then
    assertThat(personSearchRequest.pncNumber).isEqualTo("PNC1234")
    assertThat(personSearchRequest.forenameOne).isEqualTo("Stephen")
    assertThat(personSearchRequest.surname).isEqualTo("King")
    assertThat(personSearchRequest.dateOfBirth).isEqualTo(dateOfBirth)
  }

  @Test
  fun `should map common platform defendant to person search request`() {
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
    val personSearchRequest = PersonSearchRequest.from(defendant)

    // Then
    assertThat(personSearchRequest.pncNumber).isEqualTo("PNC1234")
    assertThat(personSearchRequest.forenameOne).isEqualTo("Stephen")
    assertThat(personSearchRequest.surname).isEqualTo("King")
    assertThat(personSearchRequest.dateOfBirth).isEqualTo(dateOfBirth)
  }
}
