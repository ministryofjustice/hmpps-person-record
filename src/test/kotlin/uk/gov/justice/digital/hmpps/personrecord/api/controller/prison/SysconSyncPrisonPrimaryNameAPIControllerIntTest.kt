package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SysconSyncPrisonPrimaryNameAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class ProcessPrimaryName {

    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.post()
          .uri(primaryNameUrl(randomPrisonNumber(), randomCprAliasId()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isEqualTo(501)
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.post()
          .uri(primaryNameUrl(randomPrisonNumber(), randomCprAliasId()))
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
          .uri(primaryNameUrl(randomPrisonNumber(), randomCprAliasId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  private fun randomCprAliasId() = UUID.randomUUID().toString()

  private fun primaryNameUrl(prisonNumber: String, cprAliasId: String) = "/syscon-sync/person/$prisonNumber/primary-name/$cprAliasId"
}
