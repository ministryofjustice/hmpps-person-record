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
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.FileWaiter
import uk.gov.justice.digital.hmpps.personrecord.jobs.splitclusters.RetryableClusterSplitService
import uk.gov.justice.digital.hmpps.personrecord.jobs.splitclusters.SplitClustersJob
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.jvm.java

@ExtendWith(OutputCaptureExtension::class)
class SplitClustersJobE2ETest(
  @Autowired private val retryableClusterSplitService: RetryableClusterSplitService,
  @Autowired private val fileWaiter: FileWaiter,
) : E2ETestBase() {

  private lateinit var splitClustersJob: SplitClustersJob
  private lateinit var testDir: Path

  @Autowired
  lateinit var context: ApplicationContext

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
    testDir = Files.createTempDirectory("split-clusters-test-")
    splitClustersJob = SplitClustersJob(
      personKeyRepository,
      retryableClusterSplitService,
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
      assertThat(output.out).contains("Running batch job 'SPLIT_CLUSTERS' ")
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

    @Test
    fun `should split 2 records into separate clusters`() {
      val firstPerson = createRandomProbationPersonDetails()
      val secondPerson = createRandomProbationPersonDetails()
      val cluster = createPersonKey().addPerson(firstPerson).addPerson(secondPerson)

      val clustersToSplit = listOf(cluster.personUUID.toString())

      runJobWith(clustersToSplit)

      val first = personRepository.findByCrn(firstPerson.crn!!)!!
      val second = personRepository.findByCrn(secondPerson.crn!!)!!
      assertThat(first.personKey!!.personUUID).isNotEqualTo(second.personKey!!.personUUID)

      val (leftUUID, stayedOnUUID) = when {
        first.personKey!!.personUUID != cluster.personUUID -> Pair(first, second) else -> Pair(second, first)
      }
      checkEventLog(leftUUID.crn!!, CPRLogEvents.CPR_UUID_CREATED) { eventLogs ->
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isNotEqualTo(cluster.personUUID)
      }
      checkEventLogExist(stayedOnUUID.crn!!, CPRLogEvents.CPR_UUID_CREATED, 0)

      checkEventLog(leftUUID.crn!!, CPRLogEvents.CPR_UUID_LEAVE) { eventLogs ->
        assertThat(eventLogs).isNotEmpty()
        assertThat(eventLogs.first().personUUID).isEqualTo(stayedOnUUID.personKey?.personUUID)
      }

      checkEventLogExist(leftUUID.crn!!, CPRLogEvents.CPR_UUID_SPLIT)
      checkEventLogExist(stayedOnUUID.crn!!, CPRLogEvents.CPR_UUID_SPLIT, 0)
    }

    @Test
    fun `should split records into two clusters of 2 records`() {
      val firstCrn = randomCrn()
      val secondCrn = randomCrn()
      val thirdCrn = randomCrn()
      val fourthCrn = randomCrn()
      val firstPair = createRandomProbationPersonDetails(crn = firstCrn)
      val secondPair = createRandomProbationPersonDetails(crn = thirdCrn)
      val cluster = createPersonKey()
        .addPerson(firstPair)
        .addPerson(firstPair.copy(crn = secondCrn))
        .addPerson(secondPair)
        .addPerson(secondPair.copy(crn = fourthCrn))

      val clustersToSplit = listOf(cluster.personUUID!!.toString())

      runJobWith(clustersToSplit)

      val first = personRepository.findByCrn(firstCrn)!!
      val second = personRepository.findByCrn(secondCrn)!!
      val third = personRepository.findByCrn(thirdCrn)!!
      val fourth = personRepository.findByCrn(fourthCrn)!!
      assertThat(first.personKey!!.personUUID).isEqualTo(second.personKey!!.personUUID)
      assertThat(second.personKey!!.personUUID).isNotEqualTo(third.personKey!!.personUUID)
      assertThat(third.personKey!!.personUUID).isEqualTo(fourth.personKey!!.personUUID)

      val eventLogClustersToCheck = listOf(first, second, third, fourth).filter { it.personKey!!.personUUID != cluster.personUUID }
      val eventLogClustersToCheckWithoutUUIDCreated = listOf(first, second, third, fourth).filter { it.personKey!!.personUUID == cluster.personUUID }

      val eventLogs = eventLogClustersToCheck.map { recordToCheck ->
        eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(
          CPRLogEvents.CPR_UUID_CREATED,
          recordToCheck.crn!!,
        )
      }.filter { it?.size == 1 }
      assertThat(eventLogs.size).isEqualTo(1)
      eventLogClustersToCheck.forEach { recordToCheck ->
        checkEventLogExist(recordToCheck.crn!!, CPRLogEvents.CPR_UUID_SPLIT)
      }

      val noEventLogs = eventLogClustersToCheckWithoutUUIDCreated.map { recordToCheck ->
        eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(
          CPRLogEvents.CPR_UUID_CREATED,
          recordToCheck.crn!!,
        )
      }.filter { it?.size == 1 }
      assertThat(noEventLogs.isEmpty()).isTrue()
    }

    @Test
    fun `should not split cluster if it is valid`(output: CapturedOutput) {
      val firstPerson = createRandomProbationPersonDetails()
      val secondPerson = firstPerson.copy(crn = randomCrn())
      val cluster = createPersonKey().addPerson(firstPerson).addPerson(secondPerson)

      val clustersToSplit = listOf(cluster.personUUID.toString())

      runJobWith(clustersToSplit)

      val first = personRepository.findByCrn(firstPerson.crn!!)!!
      val second = personRepository.findByCrn(secondPerson.crn!!)!!
      assertThat(first.personKey!!.personUUID).isEqualTo(second.personKey!!.personUUID)
      assertThat(output.out).contains("Cluster ${cluster.personUUID} is valid, not splitting")
    }
  }

  @Nested
  inner class MissingCluster {

    @Test
    fun `should log warning and not process anything when cluster uuid is not found`(output: CapturedOutput) {
      val nonExistentUuid = UUID.randomUUID().toString()
      runJobWith(listOf(nonExistentUuid))

      assertThat(output.out).contains("Cluster not found for person UUID: $nonExistentUuid, skipping")
    }
  }

  @Nested
  inner class MissingFile {

    @Test
    fun `should log warning and not process anything when cluster file is not found within timeout`(output: CapturedOutput) {
      val job = SplitClustersJob(
        personKeyRepository,
        retryableClusterSplitService,
        fileWaiter,
        testDir,
      )

      job.run()

      assertThat(output.out).contains("File not found after waiting")
    }
  }

  // TODO: add following tests
  // 1. consider retry (i.e. as a result of concurrent update)

  // remember to remove first line of generated uuid list

  private fun runJobWith(records: List<String>) {
    val file = testDir.resolve("clusters.csv")
    Files.write(file, records)
    splitClustersJob.run()
  }
}
