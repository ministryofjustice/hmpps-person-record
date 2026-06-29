package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
class DeliusReconciliationReport(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/deliusreconciliationreport"])
  suspend fun report(): String {
    runDeliusReconciliation()
    return OK
  }

  suspend fun runDeliusReconciliation() {
    CoroutineScope(Dispatchers.Default).launch {
      val pageSize = 1
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          pageSize,
        ),
      )?.page?.totalPages!!

      log.info("$JOB_NAME $totalPages pages with page size of $pageSize")

      val totalDeliusPersons = totalPages * pageSize
      val totalCprPersons = personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.DELIUS)

      log.info("$JOB_NAME $totalDeliusPersons Delius records, $totalCprPersons CPR records")

      applicationEventPublisher.publishEvent(
        RecordTelemetry(
          telemetryEventType = TelemetryEventType.CPR_RECORD_DELIUS_RECONCILIATION_REPORT,
          elementMap = mapOf(
            EventKeys.DELIUS to totalDeliusPersons.toString(),
            EventKeys.CPR to totalCprPersons.toString(),
          ),
        ),
      )

      log.info("$JOB_NAME Job completed successfully")
    }
  }

  companion object {
    private const val OK = "OK"
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val JOB_NAME = "JOB: deliusreconciliationreport:"
  }
}
