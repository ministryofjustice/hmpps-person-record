package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Address
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.DefendantAlias
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
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
      pncId = PNCIdentifier.from("1979/0026538X"),
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

  @Test
  fun `should map common platform defendant to person with additional fields`() {
    // Given
    val dateOfBirth = LocalDate.now()
    val defendant = Defendant(
      pncId = PNCIdentifier.from("1979/0026538X"),
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          firstName = "Stephen",
          lastName = "King",
          dateOfBirth = dateOfBirth,
          gender = "M",
          contact = Contact(
            home = "01234567890",
            mobile = "91234567890",
          ),
          address = Address(
            address1 = "1",
            postcode = "LS1 1AB",
          ),
        ),
      ),
      aliases = listOf(
        DefendantAlias(
          firstName = "Stephen",
          lastName = "Smith",
        ),
      ),
    )

    // When
    val person = Person.from(defendant)

    // Then
    assertThat(person.contacts[0].contactType).isEqualTo(ContactType.HOME)
    assertThat(person.contacts[0].contactValue).isEqualTo("01234567890")
    assertThat(person.contacts[1].contactType).isEqualTo(ContactType.MOBILE)
    assertThat(person.contacts[1].contactValue).isEqualTo("91234567890")
    assertThat(person.names[0].firstName).isEqualTo("Stephen")
    assertThat(person.names[0].lastName).isEqualTo("Smith")
    assertThat(person.addresses[0].postcode).isEqualTo("LS1 1AB")
  }
}
