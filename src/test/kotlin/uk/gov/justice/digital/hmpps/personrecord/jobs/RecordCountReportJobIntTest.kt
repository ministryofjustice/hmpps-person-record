package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class RecordCountReportJobIntTest(@Autowired val applicationEventPublisher: ApplicationEventPublisher, @Autowired val personMatchClient: PersonMatchClient) : IntegrationTestBase() {
  private lateinit var recordCountReportJob: RecordCountReportJob

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
    recordCountReportJob = RecordCountReportJob(personRepository, applicationEventPublisher, personMatchClient)
    stubPersonMatchRecordCount()
  }

  private fun stubPersonMatchRecordCount() {
    stubPostRequest(
      url = "/jobs/recordcountreport",
      status = 200,
      responseBody = "{}",
    )
  }

  @Test
  fun `should not include merged record in telemetry`() {
    val active = createPersonWithNewKey(createRandomProbationPersonDetails())
    createMergedPerson(createRandomProbationPersonDetails(), active.id)
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
