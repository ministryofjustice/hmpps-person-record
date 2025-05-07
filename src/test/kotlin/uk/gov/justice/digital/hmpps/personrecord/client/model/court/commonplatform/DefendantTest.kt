package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class DefendantTest {
  @Test
  fun `minimumDataIsPresent returns false if only lastname is persent`() {
    assertThat(Defendant(personDefendant = PersonDefendant(personDetails = PersonDetails(lastName = randomName()))).minimumDataIsPresent()).isFalse()
  }

  @Test
  fun `minimumDataIsPresent returns true if firstName and lastName are present`() {
    assertThat(Defendant(personDefendant = PersonDefendant(personDetails = PersonDetails(firstName = randomName(), lastName = randomName()))).minimumDataIsPresent()).isTrue()
  }

  @Test
  fun `minimumDataIsPresent returns true if middleName and lastName are present`() {
    assertThat(Defendant(personDefendant = PersonDefendant(personDetails = PersonDetails(middleName = randomName(), lastName = randomName()))).minimumDataIsPresent()).isTrue()
  }

  @Test
  fun `minimumDataIsPresent returns true if dateOfBirth and lastName are present`() {
    assertThat(Defendant(personDefendant = PersonDefendant(personDetails = PersonDetails(lastName = randomName(), dateOfBirth = randomDate()))).minimumDataIsPresent()).isTrue()
  }
}
