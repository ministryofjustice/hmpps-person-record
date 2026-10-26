package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterDatabaseJob
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ExtendWith(OutputCaptureExtension::class)
class ReclusterDatabaseJobTest : E2ETestBase() {

  @Autowired
  lateinit var context: ApplicationContext

  @Nested
  @TestPropertySource(properties = ["batch.enabled=true", "batch.type=RECLUSTER_DATABASE", "batch.exit-on-completion=false"])
  inner class SuccessfulProcessing {

    @Autowired
    lateinit var reclusterDatabaseJob: ReclusterDatabaseJob

    @Test
    fun `should recluster all persons`() {
      val prisonNumberOne = randomPrisonNumber()
      val prisonNumberTwo = randomPrisonNumber()
      val crnOne = randomCrn()
      val crnTwo = randomCrn()

      createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumberOne))
        .addPerson(createRandomPrisonPersonDetails(prisonNumberTwo))

      createPersonKey()
        .addPerson(createRandomProbationPersonDetails(crnOne))
        .addPerson(createRandomProbationPersonDetails(crnTwo))

      reclusterDatabaseJob.run()

      checkEventLogExist(prisonNumberOne, CPR_RECLUSTER_RECORD_MERGED, 1)
      // telemetry event (CPR_RECLUSTER_MERGE)
      // telemetry monitoring (CPR_ADMIN_RECLUSTER_TRIGGERED)
    }

    @Test
    fun `should not recluster merged and passive persons`() {
    }
  }

  @Nested
  @TestPropertySource(properties = ["batch.enabled=true", "batch.type=RECLUSTER_DATABASE", "batch.exit-on-completion=false"])
  inner class BatchEnabled {
    @Test
    fun `should register and run job when batch is enabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(ReclusterDatabaseJob::class.java)).size().isEqualTo(1)
      assertThat(output.out).contains("Running batch job 'RECLUSTER_DATABASE'")
    }
  }

  @Nested
  @TestPropertySource(properties = ["batch.enabled=false"])
  inner class BatchDisabled {
    @Test
    fun `should not register and run job when batch is disabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(ReclusterDatabaseJob::class.java)).isEmpty()
      assertThat(output.out).doesNotContain("Running batch job 'RECLUSTER_DATABASE'")
    }
  }
}
