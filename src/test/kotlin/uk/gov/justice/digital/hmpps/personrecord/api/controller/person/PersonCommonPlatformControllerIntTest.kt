package uk.gov.justice.digital.hmpps.personrecord.api.controller.person

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class PersonCommonPlatformControllerIntTest : WebTestBase() {

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return 404 when defendantId not found`() {
      webTestClient.get()
        .uri(matchDetailsUrl(randomDefendantId()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when no auth header`() {
      webTestClient.get()
        .uri(matchDetailsUrl("unauthorised"))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return FORBIDDEN 403 when role is wrong`() {
      val defendantId = randomDefendantId()
      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  private fun matchDetailsUrl(defendantId: String) = "/person/commonplatform/$defendantId/match-details"
}
