package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.FileWaiter
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED
import java.nio.file.Files
import java.nio.file.Path
import kotlin.jvm.java
@ExtendWith(OutputCaptureExtension::class)
class SplitClustersJobTest(
  @Autowired private val transactionalReclusterService: TransactionalReclusterService,
  @Autowired private val personRepo: PersonRepository,
  @Autowired private val personMatchService: PersonMatchService,
  @Autowired private val publisher: ApplicationEventPublisher,
  @Autowired private val fileWaiter: FileWaiter,
) : IntegrationTestBase() {

  private lateinit var splitClustersJob: SplitClustersJob
  private lateinit var testDir: Path

  @Autowired
  lateinit var context: ApplicationContext

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
    testDir = Files.createTempDirectory("recluster-test-")
    splitClustersJob = SplitClustersJob(
      jsonMapper,
      fileWaiter,
      testDir,
    )
  }

  @AfterEach
  fun tearDown() {
    testDir.toFile().deleteRecursively()
  }

  @TestPropertySource(properties = ["batch.enabled=true", "batch.type=SPLIT_CLUSTERS", "batch.exit-on-completion=false"])
  @Nested
  inner class BatchEnabled {
    @Test
    fun `should register and run job when batch is enabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(SplitClustersJob::class.java)).size().isEqualTo(1)
      assertThat(output.out).contains("R  unning cluster split job")
    }
  }

  @TestPropertySource(properties = ["batch.enabled=false"])
  @Nested
  inner class BatchDisabled {
    @Test
    fun `should not register and run job when batch is disabled`(output: CapturedOutput) {
      assertThat(context.getBeansOfType(SplitClustersJob::class.java)).isEmpty()
      assertThat(output.out).doesNotContain("Running cluster split job")
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should split record single person record`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val records = listOf(ReclusterRecord(DELIUS, person.crn!!))

      runJobWith(records)

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
    }
  }
  private fun runJobWith(records: List<ReclusterRecord>) {
    val file = testDir.resolve("splitClusters.json")
    Files.writeString(file, jsonMapper.writeValueAsString(records))
    splitClustersJob.run()
  }
}
