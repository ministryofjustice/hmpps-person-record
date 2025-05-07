package uk.gov.justice.digital.hmpps.personrecord.client.model.court.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class LibraHearingEventTest {

  @Test
  fun `minimumDataIsPresent false if only lastname is present`() {
    assertThat(LibraHearingEvent(name = Name(lastName = randomName())).minimumDataIsPresent()).isFalse()
  }

  @Test
  fun `minimumDataIsPresent true if firstname and lastname are present`() {
    assertThat(LibraHearingEvent(name = Name(firstName = randomName(), lastName = randomName())).minimumDataIsPresent()).isTrue()
  }

  @Test
  fun `minimumDataIsPresent true if forename2 and lastname are present`() {
    assertThat(LibraHearingEvent(name = Name(forename2 = randomName(), lastName = randomName())).minimumDataIsPresent()).isTrue()
  }

  @Test
  fun `minimumDataIsPresent true if forename3 and lastname are present`() {
    assertThat(LibraHearingEvent(name = Name(forename3 = randomName(), lastName = randomName())).minimumDataIsPresent()).isTrue()
  }

  @Test
  fun `minimumDataIsPresent true if dateOfBirth and lastname are present`() {
    assertThat(LibraHearingEvent(name = Name(lastName = randomName()), dateOfBirth = randomDate()).minimumDataIsPresent()).isTrue()
  }
}
