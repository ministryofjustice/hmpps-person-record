package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.ReclusterDatabaseJob
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ExtendWith(OutputCaptureExtension::class)
class ReclusterDatabaseJobE2ETest(
  @Autowired private val transactionalReclusterService: TransactionalReclusterService,
  @Autowired private val publisher: ApplicationEventPublisher,
) : E2ETestBase() {

  private lateinit var reclusterDatabaseJob: ReclusterDatabaseJob

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    reclusterDatabaseJob = ReclusterDatabaseJob(
      personRepository = personRepository,
      transactionalReclusterService = transactionalReclusterService,
      publisher = publisher,
      batchSize = 50,
      startPage = 0,
      endPageInclusive = Int.MAX_VALUE,
    )
  }

  @Test
  fun `should recluster all persons`() {
    val prisonNumberOne = randomPrisonNumber()
    val prisonNumberTwo = randomPrisonNumber()
    val crnOne = randomCrn()
    val crnTwo = randomCrn()

    val clusterOne = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonNumberTwo))

    val clusterTwo = createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crnOne))
      .addPerson(createRandomProbationPersonDetails(crnTwo))

    reclusterDatabaseJob.run()

    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterOne.personUUID.toString()), times = 2)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterTwo.personUUID.toString()), times = 2)
  }

  @Test
  fun `should not recluster merged and passive persons`() {
    val fromCrn = randomPrisonNumber()
    val toCrn = randomPrisonNumber()

    val fromCluster = createPersonKey()
      .addPerson(createRandomProbationPersonDetails(fromCrn))

    val toCluster = createPersonKey()
      .addPerson(createRandomProbationPersonDetails(toCrn))

    val passiveCluster = createPersonKey()
      .addPerson(createRandomProbationPersonDetails(randomCrn()))
    val passivePerson = passiveCluster.personEntities.first()
    passivePerson.markAsPassive()
    personRepository.save(passivePerson)

    probationMergeEventAndResponseSetup(sourceCrn = fromCrn, targetCrn = toCrn)
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "FROM_SOURCE_SYSTEM_ID" to fromCrn,
        "TO_SOURCE_SYSTEM_ID" to toCrn,
      ),
    )

    reclusterDatabaseJob.run()

    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to fromCluster.personUUID.toString()), times = 0)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to toCluster.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to passiveCluster.personUUID.toString()), times = 0)
  }

  @Test
  fun `should stop when last page is complete`() {
    reclusterDatabaseJob = ReclusterDatabaseJob(
      personRepository = personRepository,
      transactionalReclusterService = transactionalReclusterService,
      publisher = publisher,
      batchSize = 2,
      startPage = 0,
      endPageInclusive = Int.MAX_VALUE,
    )

    val clusterOne = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterTwo = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterThree = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterFour = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))

    reclusterDatabaseJob.run()

    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterOne.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterTwo.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterThree.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterFour.personUUID.toString()), times = 1)
  }

  @Test
  fun `should stop when end page is completed even when more persons existsss`() {
    reclusterDatabaseJob = ReclusterDatabaseJob(
      personRepository = personRepository,
      transactionalReclusterService = transactionalReclusterService,
      publisher = publisher,
      batchSize = 2,
      startPage = 0,
      endPageInclusive = 1, // <-- zero based (2nd page)
    )

    val clusterOne = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterTwo = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))

    val clusterThree = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterFour = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))

    val clusterFive = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))
    val clusterSix = createPersonKey().addPerson(createRandomPrisonPersonDetails(randomPrisonNumber()))

    reclusterDatabaseJob.run()

    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterOne.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterTwo.personUUID.toString()), times = 1)

    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterThree.personUUID.toString()), times = 1)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterFour.personUUID.toString()), times = 1)

    // these would have been done on page 2 (3rd page)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterFive.personUUID.toString()), times = 0)
    checkTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, mapOf("UUID" to clusterSix.personUUID.toString()), times = 0)
  }
}
