package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.FileWaiter
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterRecordsJob
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import java.nio.file.Files
import java.nio.file.Path

class ReclusterRecordsJobIntTest(
  @Autowired private val transactionalReclusterService: TransactionalReclusterService,
  @Autowired private val personRepo: PersonRepository,
  @Autowired private val personMatchService: PersonMatchService,
  @Autowired private val publisher: ApplicationEventPublisher,
  @Autowired private val fileWaiter: FileWaiter,
  @Autowired private val objectMapper: ObjectMapper,
) : IntegrationTestBase() {

  private lateinit var testDir: Path
  private lateinit var reclusterRecordsJob: ReclusterRecordsJob

  @BeforeEach
  fun beforeEach() {
    testDir = Files.createTempDirectory("recluster-test-")
    reclusterRecordsJob = ReclusterRecordsJob(
      transactionalReclusterService,
      personRepo,
      personMatchService,
      publisher,
      fileWaiter,
      objectMapper,
      testDir,
    )
  }

  @AfterEach
  fun tearDown() {
    testDir.toFile().deleteRecursively()
  }

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not do anything when person record not found in list`() {
      val defendantId = randomDefendantId()
      val records = listOf(ReclusterRecord(COMMON_PLATFORM, defendantId))

      configureInputAndRun(records)

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to defendantId),
        times = 0,
      )
    }

    @Test
    fun `should not recluster records that have been merged`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPerson = createPerson(createRandomProbationPersonDetails()) { mergedTo = person.id }

      mergedPerson.assertMergedTo(person)

      val records = listOf(ReclusterRecord(DELIUS, mergedPerson.crn!!))

      configureInputAndRun(records)

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
    }
  }

  @Nested
  inner class ErrorRecovery {

    @Test
    fun `should retry if request to hmpps-person-match fails`() {
      stubPersonMatchUpsert()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val records = listOf(ReclusterRecord(DELIUS, person.crn!!))
      stub5xxResponse(url = "/person/score/" + person.matchId)
      stubPersonMatchScores(
        matchId = person.matchId,
        personMatchResponse = listOf(),
        currentScenarioState = "Next request will succeed",
      )

      configureInputAndRun(records)

      person.personKey?.assertClusterStatus(ACTIVE)
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
    fun `should recluster single person record`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val records = listOf(ReclusterRecord(DELIUS, person.crn!!))

      configureInputAndRun(records)

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
    }

    @Test
    fun `should recluster multiple person records`() {
      val recordsWithCluster = List(5) {
        createPersonWithNewKey(createRandomProbationPersonDetails())
      }
      val records = recordsWithCluster.map { ReclusterRecord(it.sourceSystem, it.crn!!) }

      configureInputAndRun(records)

      recordsWithCluster.forEach {
        checkTelemetry(
          CPR_ADMIN_RECLUSTER_TRIGGERED,
          mapOf("UUID" to it.personKey?.personUUID.toString()),
        )
      }
    }

    @Test
    fun `should set needs attention to active when cluster is valid`() {
      val person =
        createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val records = listOf(ReclusterRecord(DELIUS, person.crn!!))

      configureInputAndRun(records)

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
      person.personKey?.assertClusterStatus(ACTIVE)
    }
  }

  private fun configureInputAndRun(records: List<ReclusterRecord>) {
    val file = testDir.resolve("recluster.json")
    Files.writeString(file, objectMapper.writeValueAsString(records))
    reclusterRecordsJob.run()
  }
}
