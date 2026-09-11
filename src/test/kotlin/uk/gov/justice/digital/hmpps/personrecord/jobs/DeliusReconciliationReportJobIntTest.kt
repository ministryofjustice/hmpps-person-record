package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.web.PagedModel.PageMetadata
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class DeliusReconciliationReportJobIntTest(
  @Autowired corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Autowired applicationEventPublisher: ApplicationEventPublisher,
  @Autowired personRepo: PersonRepository,
) : IntegrationTestBase() {

  val deliusReconciliationReportJob = DeliusReconciliationReportJob(corePersonRecordAndDeliusClient, applicationEventPublisher, personRepo)

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should track person count between delius and cpr`() {
    createPersonWithNewKey(createRandomProbationPersonDetails())

    val responseBody = ProbationCases(
      page = PageMetadata(1, 0, 0, 5),
    )
    stubGetRequest(
      url = "/all-probation-cases?page=0&size=1&sort=id,asc",
      body = jsonMapper.writeValueAsString(responseBody),
    )

    deliusReconciliationReportJob.run()

    checkTelemetry(
      TelemetryEventType.CPR_RECORD_DELIUS_RECONCILIATION_REPORT,
      mapOf(
        "DELIUS" to "5",
        "CPR" to "1",
      ),
    )
  }
}
