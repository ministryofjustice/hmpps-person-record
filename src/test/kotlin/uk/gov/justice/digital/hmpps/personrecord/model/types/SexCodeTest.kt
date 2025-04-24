package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import kotlin.test.assertEquals

class SexCodeTest {

  @Nested
  inner class FromCommonPlatformMessages {

    @Test
    fun `should map from MALE gender to M sexCode`() {
      val personDetails = PersonDetails(
        gender = "MALE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.M)
    }

    @Test
    fun `should map from FEMALE gender to F sexCode`() {
      val personDetails = PersonDetails(
        gender = "FEMALE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.F)
    }

    @Test
    fun `should map from NOT SPECIFIED gender to NS sexCode`() {
      val personDetails = PersonDetails(
        gender = "NOT SPECIFIED",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.NS)
    }

    @Test
    fun `should map from ANYTHING ELSE gender to N sexCode`() {
      val personDetails = PersonDetails(
        gender = "ANYTHING ELSE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.N)
    }

    @Test
    fun `should map from null gender to null sexCode`() {
      val personDetails = PersonDetails(
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), null)
    }
  }

  @Nested
  inner class FromLibraCourtEvent {

    @Test
    fun `should map from M defendantSex to M sexCode`() {
      val libraHearingEvent = LibraHearingEvent(defendantSex = "M")

      assertEquals(SexCode.from(libraHearingEvent), SexCode.M)
    }

    @Test
    fun `should map from F defendantSex to F sexCode`() {
      val libraHearingEvent = LibraHearingEvent(defendantSex = "F")

      assertEquals(SexCode.from(libraHearingEvent), SexCode.F)
    }

    @Test
    fun `should map from NOT SPECIFIED defendantSex to NS sexCode`() {
      val libraHearingEvent = LibraHearingEvent(defendantSex = "NS")

      assertEquals(SexCode.from(libraHearingEvent), SexCode.NS)
    }

    @Test
    fun `should map from ANYTHING ELSE defendantSex to N sexCode`() {
      val libraHearingEvent = LibraHearingEvent(defendantSex = "ANYTHING ELSE")

      assertEquals(SexCode.from(libraHearingEvent), SexCode.N)
    }

    @Test
    fun `should map from null defendantSex to null sexCode`() {
      val libraHearingEvent = LibraHearingEvent()

      assertEquals(SexCode.from(libraHearingEvent), null)
    }
  }
}
