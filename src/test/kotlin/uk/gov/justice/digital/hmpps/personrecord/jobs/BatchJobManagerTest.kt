package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.CourtEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison.PrisonEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationAddressFromCprEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationDeleteEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationMergeEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas.SasEventListener

@TestPropertySource(properties = ["batch.enabled=true", "batch.type=ANY", "batch.exit-on-completion=false"])
@ExtendWith(OutputCaptureExtension::class)
class BatchJobManagerTest : IntegrationTestBase() {

  @Autowired
  lateinit var context: ApplicationContext

  @Test
  fun `should remove sqs beans from application context when batch is enabled`() {
    assertThat(context.getBeansOfType(CourtEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(PrisonEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(ProbationEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(ProbationDeleteEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(ProbationMergeEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(ProbationAddressFromCprEventListener::class.java)).isEmpty()
    assertThat(context.getBeansOfType(SasEventListener::class.java)).isEmpty()
  }

  @Test
  fun `should error when job is not found`(output: CapturedOutput) {
    assertThat(output.out).contains("Job 'ANY' not found")
  }
}
