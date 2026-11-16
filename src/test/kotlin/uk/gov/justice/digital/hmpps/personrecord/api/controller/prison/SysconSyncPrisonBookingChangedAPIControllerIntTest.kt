package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SysconSyncPrisonBookingChangedAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class ProcessWorkingName {

    @Nested
    inner class HappyPath {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.post()
          .uri(workingNameUrl(randomPrisonNumber(), randomCprPseudonymId()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isEqualTo(501)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.post()
          .uri(workingNameUrl(randomPrisonNumber(), randomCprPseudonymId()))
          .authorised(listOf("UNSUPPORTED-ROLE"))
          .exchange()
          .expectStatus()
          .isForbidden
          .expectBody()
          .jsonPath("userMessage")
          .isEqualTo("Forbidden: Access Denied")
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        webTestClient.post()
          .uri(workingNameUrl(randomPrisonNumber(), randomCprPseudonymId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  private fun randomCprPseudonymId() = UUID.randomUUID().toString()

  private fun workingNameUrl(prisonNumber: String, cprPseudonymId: String) = "/syscon-sync/person/$prisonNumber/working-name/$cprPseudonymId"
}
