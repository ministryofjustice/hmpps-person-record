package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ExtendWith(OutputCaptureExtension::class)
class PrisonMergeEventListenerIntTest : PrisonEventListenerTestBase() {
  @Test
  fun `should log merge details when prison merge is submitted`(output: CapturedOutput) {
    val prisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()

    publishPrisonPersonMergedEvent(prisonNumber, sourcePrisonNumber)

    assertThat(output.all).contains("Ignoring merge event")
  }
}
