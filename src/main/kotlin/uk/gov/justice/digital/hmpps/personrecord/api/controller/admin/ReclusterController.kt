package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
class ReclusterController(
  private val reclusterService: ReclusterService,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  @Hidden
  @PostMapping("/admin/recluster")
  suspend fun postRecluster(
    @RequestBody adminReclusterRecords: List<AdminReclusterRecord>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("$RECLUSTER_PROCESS_PREFIX Triggered. Number of records: ${adminReclusterRecords.size}.")
      upsertRecords(adminReclusterRecords)
      log.info("$RECLUSTER_PROCESS_PREFIX Records Upsert Complete.")
      triggerRecluster(adminReclusterRecords)
      log.info("$RECLUSTER_PROCESS_PREFIX Complete.")
    }
  }

  private fun upsertRecords(adminReclusterRecords: List<AdminReclusterRecord>) {
    adminReclusterRecords.forEachPersonAndLog(UPSERT_PROCESS_NAME) { person ->
      personMatchService.saveToPersonMatch(person)
    }
  }

  private fun triggerRecluster(adminReclusterRecords: List<AdminReclusterRecord>) {
    adminReclusterRecords.forEachPersonAndLog(RECLUSTER_PROCESS_NAME) { person ->
      person.personKey?.let { cluster ->
        publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, cluster))
        reclusterService.recluster(cluster, person)
      }
    }
  }

  private fun searchForPersonByIdentifier(record: AdminReclusterRecord): PersonEntity? = when (record.sourceSystem) {
    SourceSystemType.COMMON_PLATFORM -> personRepository.findByDefendantId(record.sourceSystemId)
    SourceSystemType.LIBRA -> personRepository.findByCId(record.sourceSystemId)
    SourceSystemType.NOMIS -> personRepository.findByPrisonNumber(record.sourceSystemId)
    SourceSystemType.DELIUS -> personRepository.findByCrn(record.sourceSystemId)
    else -> null
  }

  private fun List<AdminReclusterRecord>.forEachPersonAndLog(processName: String, action: (PersonEntity) -> Unit) {
    val total = this.count()
    log.info("Starting $processName, count: $total")
    this.forEachIndexed { idx, record ->
      val itemNumber = idx + 1
      log.info("Processing $processName, item: $itemNumber/$total")
      searchForPersonByIdentifier(record)?.let {
        action(it)
      } ?: log.info("Error $processName, record not found. id: ${record.sourceSystemId} item: $itemNumber/$total")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val RECLUSTER_PROCESS_PREFIX = "ADMIN RECLUSTER: "
    private const val UPSERT_PROCESS_NAME = "Upsert Person Records"
    private const val RECLUSTER_PROCESS_NAME = "Recluster Person Records"
  }
}
