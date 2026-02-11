package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.SysconSyncControllerIntTest.Companion.buildRequestBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconSyncFeatureFlagTest : WebTestBase() {

  @Nested
  @ActiveProfiles("dev")
  inner class Dev {

    @Test
    fun `is available in dev`() {
      stubPersonMatchUpsert()
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      webTestClient.put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(buildRequestBody()), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class Preprod {

    @Test
    fun `not available in preprod`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      webTestClient.put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(buildRequestBody()), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @ActiveProfiles("prod")
  inner class Prod {
    @Test
    fun `not available in prod`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      webTestClient.put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(buildRequestBody()), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}