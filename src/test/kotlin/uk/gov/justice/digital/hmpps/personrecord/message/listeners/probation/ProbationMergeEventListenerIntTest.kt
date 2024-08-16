package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  @Test
  fun `processes offender merge event is published`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    val source = ApiResponseSetup(crn = sourceCrn)
    val target = ApiResponseSetup(crn = targetCrn)
    probationMergeEventAndResponseSetup(OFFENDER_MERGED, source, target)

    checkTelemetry(MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"))
  }
}
