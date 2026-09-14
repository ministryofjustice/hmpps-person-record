package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.new
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class PersonTest {

  @Test
  fun `isPerson false if only lastname is present and other names are null`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(lastName = randomName()))).isPerson()).isFalse()
  }

  @Test
  fun `isPerson false if only lastname is present and other names are empty strings`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(firstName = "", forename2 = "", forename3 = "", lastName = randomName()))).isPerson()).isFalse()
  }

  @Test
  fun `isPerson true if firstname and lastname are present`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(firstName = randomName(), lastName = randomName()))).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if forename2 and lastname are present`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(forename2 = randomName(), lastName = randomName()))).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if forename3 and lastname are present`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(forename3 = randomName(), lastName = randomName()))).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if dateOfBirth and lastname are present`() {
    assertThat(Person.from(LibraHearingEvent(defendantType = PERSON.value, name = Name(lastName = randomName()), dateOfBirth = randomDate())).isPerson()).isTrue()
  }

  @Test
  fun `updateChildEntities should not update pseudonyms or references when specified`() {
    val personEntity = new(SourceSystemType.NOMIS)
    personEntity.updateChildEntities(
      Person(
        firstName = "firstName",
        references = listOf(Reference(identifierType = IdentifierType.PNC, identifierValue = "identifierValue")),
        sourceSystem = SourceSystemType.NOMIS,
      ),
      setOf(PseudonymEntity::class, ReferenceEntity::class),
    )

    assertThat(personEntity.references).hasSize(0)
    assertThat(personEntity.pseudonyms).hasSize(0)

    personEntity.updateChildEntities(
      Person(
        firstName = "firstName",
        references = listOf(Reference(identifierType = IdentifierType.PNC, identifierValue = "identifierValue")),
        sourceSystem = SourceSystemType.NOMIS,
      ),
    )

    assertThat(personEntity.references).hasSize(1)
    assertThat(personEntity.pseudonyms).hasSize(1)
  }
}
