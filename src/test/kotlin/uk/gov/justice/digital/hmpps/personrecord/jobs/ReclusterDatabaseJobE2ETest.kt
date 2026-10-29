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
    reclusterDatabaseJob = ReclusterDatabaseJob(
      personRepository = personRepository,
      transactionalReclusterService = transactionalReclusterService,
      publisher = publisher,
      batchSize = 50,
      startPage = 0,
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
}
