package uk.gov.justice.digital.hmpps.personrecord.client.model.court.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class LibraHearingEventTest {

  @Test
  fun `isPerson false if only lastname is present and other names are null`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(lastName = randomName())).isPerson()).isFalse()
  }

  @Test
  fun `isPerson false if only lastname is present and other names are empty strings`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(firstName = "", forename2 = "", forename3 = "", lastName = randomName())).isPerson()).isFalse()
  }

  @Test
  fun `isPerson true if firstname and lastname are present`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(firstName = randomName(), lastName = randomName())).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if forename2 and lastname are present`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(forename2 = randomName(), lastName = randomName())).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if forename3 and lastname are present`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(forename3 = randomName(), lastName = randomName())).isPerson()).isTrue()
  }

  @Test
  fun `isPerson true if dateOfBirth and lastname are present`() {
    assertThat(LibraHearingEvent(defendantType = PERSON.value, name = Name(lastName = randomName()), dateOfBirth = randomDate()).isPerson()).isTrue()
  }
}
