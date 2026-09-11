package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class DeliusReconciliationReportJob(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val personRepository: PersonRepository,
) : BatchJob {
  override val jobName = "DELIUS_RECONCILIATION_REPORT"

  override fun run() {
    val pageSize = 1
    val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
      CorePersonRecordAndDeliusClientPageParams(
        0,
        pageSize,
      ),
    )?.page?.totalPages!!

    log.info("$jobName $totalPages pages with page size of $pageSize")

    val totalDeliusPersons = totalPages * pageSize
    val totalCprPersons = personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.DELIUS)

    log.info("$jobName $totalDeliusPersons Delius records, $totalCprPersons CPR records")

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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
