package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ExtendWith(OutputCaptureExtension::class)
class SysconSyncPrisonMergeAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should do nothing when prison merge is submitted`() {
      val prisonNumber = randomPrisonNumber()

      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(prisonNumber),
        body = PrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NO_CONTENT,
      )
    }

    @Test
    fun `should log merge details when prison merge is submitted`(output: CapturedOutput) {
      val prisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(prisonNumber),
        body = PrisonMerge(fromPrisonNumber = fromPrisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NO_CONTENT,
      )

      assertThat(output.all).contains("Ignoring prison merge for prison number: $prisonNumber from prison number: $fromPrisonNumber")
    }
  }

  @Nested
  inner class Authorisation {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = PrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = PrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"
}
