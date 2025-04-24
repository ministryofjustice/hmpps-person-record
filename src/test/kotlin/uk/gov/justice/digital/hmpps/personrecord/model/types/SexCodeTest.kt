package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import kotlin.test.assertEquals

class SexCodeTest {

  @Nested
  inner class FromCommonPlatformMessages {
    @Test
    fun `should map from MALE gender to M sex code`() {
      val personDetails = PersonDetails(
        gender = "MALE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.M)
    }

    @Test
    fun `should map from FEMALE gender to F sex code`() {
      val personDetails = PersonDetails(
        gender = "FEMALE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.F)
    }

    @Test
    fun `should map from NOT SPECIFIED gender to NS sex code`() {
      val personDetails = PersonDetails(
        gender = "NOT SPECIFIED",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.NS)
    }

    @Test
    fun `should map from ANYTHING ELSE gender to N sex code`() {
      val personDetails = PersonDetails(
        gender = "ANYTHING ELSE",
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), SexCode.N)
    }

    @Test
    fun `should map from null gender to null sex code`() {
      val personDetails = PersonDetails(
        lastName = "",
      )

      assertEquals(SexCode.from(personDetails), null)
    }
  }
}

