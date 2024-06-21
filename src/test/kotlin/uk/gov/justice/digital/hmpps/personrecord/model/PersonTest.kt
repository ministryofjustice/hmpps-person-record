package uk.gov.justice.digital.hmpps.personrecord.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.Address
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.Contact
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.DefendantAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.time.LocalDate

internal class PersonTest {

  @Test
  fun `should map libra hearing to person`() {
    val dateOfBirth = LocalDate.now()
    val inputPncId = randomPnc()
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val libraHearingEvent = LibraHearingEvent(
      pnc = PNCIdentifier.from(inputPncId),
      name = Name(title = "Mr", firstName = firstName, lastName = lastName),
      dateOfBirth = dateOfBirth,
    )

    val person = Person.from(libraHearingEvent)

    assertThat(person.otherIdentifiers?.pncIdentifier).isEqualTo(PNCIdentifier.from(inputPncId))
    assertThat(person.firstName).isEqualTo(firstName)
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
  }

  @Test
  fun `should map common platform defendant to person with additional fields`() {
    val dateOfBirth = LocalDate.now()
    val inputPncId = randomPnc()
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val defendant = Defendant(
      pncId = PNCIdentifier.from(inputPncId),
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          firstName = firstName,
          lastName = lastName,
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
          firstName = firstName,
          lastName = "Smith",
        ),
      ),
    )

    // When
    val person = Person.from(defendant)

    // Then
    assertThat(person.contacts[0].contactType).isEqualTo(HOME)
    assertThat(person.contacts[0].contactValue).isEqualTo("01234567890")
    assertThat(person.contacts[1].contactType).isEqualTo(MOBILE)
    assertThat(person.contacts[1].contactValue).isEqualTo("91234567890")
    assertThat(person.aliases[0].firstName).isEqualTo(firstName)
    assertThat(person.aliases[0].lastName).isEqualTo("Smith")
    assertThat(person.addresses[0].postcode).isEqualTo("LS1 1AB")
  }
}
