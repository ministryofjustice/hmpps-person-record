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
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterDatabaseJob

@ExtendWith(OutputCaptureExtension::class)
class ReclusterDatabaseJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var context: ApplicationContext

  @TestPropertySource(properties = ["batch.enabled=true", "batch.type=RECLUSTER_DATABASE", "batch.exit-on-completion=false"])
  @Nested
  inner class BatchEnabled {
    @Test
    fun `should register and run job when batch is enabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(ReclusterDatabaseJob::class.java)).size().isEqualTo(1)
      assertThat(output.out).contains("Running batch job 'RECLUSTER_DATABASE'")
    }
  }

  @TestPropertySource(properties = ["batch.enabled=false"])
  @Nested
  inner class BatchDisabled {
    @Test
    fun `should not register and run job when batch is disabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(ReclusterDatabaseJob::class.java)).isEmpty()
      assertThat(output.out).doesNotContain("Running batch job 'RECLUSTER_DATABASE'")
    }
  }
}
