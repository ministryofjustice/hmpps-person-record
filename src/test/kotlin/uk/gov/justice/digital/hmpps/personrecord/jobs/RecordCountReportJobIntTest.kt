package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class RecordCountReportJobIntTest(@Autowired applicationEventPublisher: ApplicationEventPublisher, @Autowired personRepo: PersonRepository) : IntegrationTestBase() {

  val recordCountReportJob = RecordCountReportJob(personRepo, applicationEventPublisher)

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should not include merged record in telemetry`() {
    val active = createPersonWithNewKey(createRandomProbationPersonDetails())
    createPerson(createRandomProbationPersonDetails()) { mergedTo = active.id }
    createPersonWithNewKey(createRandomPrisonPersonDetails())
    createPersonWithNewKey(createRandomLibraPersonDetails())
    createPersonWithNewKey(createRandomCommonPlatformPersonDetails())
    recordCountReportJob.run()
    checkTelemetry(
      TelemetryEventType.CPR_RECORD_COUNT_REPORT,
      mapOf(
        "NOMIS" to "1",
        "DELIUS" to "1",
        "COMMON_PLATFORM" to "1",
        "LIBRA" to "1",
      ),
    )
  }

  @Test
  fun `should not include passive state records in telemetry`() {
    createPersonWithNewKey(createRandomPrisonPersonDetails()) { markAsPassive() }
    createPersonWithNewKey(createRandomPrisonPersonDetails())
    createPersonWithNewKey(createRandomProbationPersonDetails())
    createPersonWithNewKey(createRandomLibraPersonDetails())
    createPersonWithNewKey(createRandomCommonPlatformPersonDetails())
    recordCountReportJob.run()
    checkTelemetry(
      TelemetryEventType.CPR_RECORD_COUNT_REPORT,
      mapOf(
        "NOMIS" to "1",
        "DELIUS" to "1",
        "COMMON_PLATFORM" to "1",
        "LIBRA" to "1",
      ),
    )
  }
}
