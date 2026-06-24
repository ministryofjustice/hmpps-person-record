package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
  fun report(): String {
    CoroutineScope(Dispatchers.Default).launch {
      runDeliusReconciliation()
    }
    return "OK"
  }

  suspend fun runDeliusReconciliation() {
    val pageSize = 1
    val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
      CorePersonRecordAndDeliusClientPageParams(
        0,
        1,
      ),
    )?.page?.totalPages ?: 1

    val totalDeliusPersons = totalPages * pageSize
    val totalCprPersons = personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.DELIUS)

    applicationEventPublisher.publishEvent(
      RecordTelemetry(
        telemetryEventType = TelemetryEventType.CPR_RECORD_DELIUS_RECONCILIATION_REPORT,
        elementMap = mapOf(
          EventKeys.DELIUS to totalDeliusPersons.toString(),
          EventKeys.CPR to totalCprPersons.toString(),
        ),
      ),
    )
  }
}
