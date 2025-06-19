package uk.gov.justice.digital.hmpps.personrecord.api.controller

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
@RequestMapping("/admin")
class AdminController(
  private val reclusterService: ReclusterService,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryClient: TelemetryClient,
) {

  @Hidden
  @PostMapping("/recluster")
  suspend fun postRecluster(
    @RequestBody adminReclusterRecords: List<AdminReclusterRecord>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("$RECLUSTER_PROCESS_PREFIX Triggered. Number of records: ${adminReclusterRecords.size}.")
      upsertRecords(adminReclusterRecords)
      log.info("$RECLUSTER_PROCESS_PREFIX Records Upsert Complete.")
      val reclusterResult = triggerRecluster(adminReclusterRecords)
      log.info("$RECLUSTER_PROCESS_PREFIX Complete. Summary: $reclusterResult")
      telemetryClient.trackEvent(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_SUMMARY.eventName,
        reclusterResult.entries.associate { it.key to it.value.toString() },
        null,
      )
    }
  }

  private fun upsertRecords(adminReclusterRecords: List<AdminReclusterRecord>) {
    adminReclusterRecords.forEachPersonAndLog(UPSERT_PROCESS_NAME) { person ->
      personMatchService.saveToPersonMatch(person)
    }
  }

  private fun triggerRecluster(adminReclusterRecords: List<AdminReclusterRecord>): Map<String, Int> {
    val reclusterResultMap = mutableMapOf<String, Int>()
    val notFoundCount = adminReclusterRecords.forEachPersonAndLog(RECLUSTER_PROCESS_NAME) { person ->
      person.personKey?.let { cluster ->
        val beforeStatus = cluster.status.name
        reclusterService.recluster(cluster, person)
        val afterStatus = personKeyRepository.findByPersonUUID(cluster.personUUID)!!.status.name
        val key = "$beforeStatus -> $afterStatus"
        reclusterResultMap[key] = reclusterResultMap.getOrPut(key) { 0 } + 1
      }
    }
    reclusterResultMap[NOT_FOUND_RECORDS] = notFoundCount
    return reclusterResultMap.toMap()
  }

  private fun searchForPersonByIdentifier(record: AdminReclusterRecord): PersonEntity? = when (record.sourceSystem) {
    SourceSystemType.COMMON_PLATFORM -> personRepository.findByDefendantId(record.sourceSystemId)
    SourceSystemType.LIBRA -> personRepository.findByCId(record.sourceSystemId)
    SourceSystemType.NOMIS -> personRepository.findByPrisonNumber(record.sourceSystemId)
    SourceSystemType.DELIUS -> personRepository.findByCrn(record.sourceSystemId)
    else -> null
  }

  private fun List<AdminReclusterRecord>.forEachPersonAndLog(processName: String, action: (PersonEntity) -> Unit): Int {
    val total = this.count()
    var notFoundCount = 0
    log.info("Starting $processName, count: $total")
    this.forEachIndexed { idx, record ->
      val itemNumber = idx + 1
      log.info("Processing $processName, item: $itemNumber/$total")
      searchForPersonByIdentifier(record)?.let {
        action(it)
      } ?: run {
        log.info("Error $processName, record not found. id: ${record.sourceSystemId} item: $itemNumber/$total")
        notFoundCount++
      }
    }
    return notFoundCount
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private const val NOT_FOUND_RECORDS = "NOT_FOUND_RECORDS"
    private const val RECLUSTER_PROCESS_PREFIX = "ADMIN RECLUSTER: "
    private const val UPSERT_PROCESS_NAME = "Upsert Person Records"
    private const val RECLUSTER_PROCESS_NAME = "Recluster Person Records"
  }
}
