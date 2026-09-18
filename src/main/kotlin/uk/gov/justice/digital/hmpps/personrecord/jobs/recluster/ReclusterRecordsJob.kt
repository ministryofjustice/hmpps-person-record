package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.jobs.BatchJob
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.nio.file.Path

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class ReclusterRecordsJob(
  private val transactionalReclusterService: TransactionalReclusterService,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
  private val fileWaiter: FileWaiter,
  private val jsonMapper: JsonMapper,
  dataDir: Path = Path.of("/data"),
) : BatchJob {
  override val jobName = "RECLUSTER_RECORDS"
  private val path = dataDir.resolve("recluster.json")

  override fun run() {
    val file = runBlocking {
      fileWaiter.waitFor(path)
    } ?: run {
      log.warn("File not found after waiting: {}", path)
      return
    }

    val reclusterRecords: List<ReclusterRecord> = jsonMapper.readValue(file.toFile())

    log.info("$jobName Triggered. Number of records: ${reclusterRecords.size}.")
    upsertRecords(reclusterRecords)
    log.info("$jobName Records Upsert Complete.")
    triggerRecluster(reclusterRecords)
    log.info("$jobName Complete.")
  }

  private fun upsertRecords(reclusterRecords: List<ReclusterRecord>) {
    reclusterRecords.forEachPersonAndLog(UPSERT_PROCESS_NAME) { person ->
      personMatchService.saveToPersonMatch(person)
    }
  }

  fun triggerRecluster(reclusterRecords: List<ReclusterRecord>) {
    reclusterRecords.forEachPersonAndLog(RECLUSTER_PROCESS_NAME) {
      it.personKey?.let { cluster -> publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, cluster)) }
      transactionalReclusterService.recluster(it)
    }
  }

  private fun searchForPersonByIdentifier(record: ReclusterRecord): PersonEntity? = when (record.sourceSystem) {
    SourceSystemType.COMMON_PLATFORM -> personRepository.findByDefendantId(record.sourceSystemId)
    SourceSystemType.LIBRA -> personRepository.findByCId(record.sourceSystemId)
    SourceSystemType.NOMIS -> personRepository.findByPrisonNumber(record.sourceSystemId)
    SourceSystemType.DELIUS -> personRepository.findByCrn(record.sourceSystemId)
  }

  private fun List<ReclusterRecord>.forEachPersonAndLog(processName: String, action: (PersonEntity) -> Unit) {
    val total = this.count()
    log.info("Starting $processName, count: $total")
    this.forEachIndexed { idx, record ->
      val itemNumber = idx + 1
      log.info("Processing $processName, item: $itemNumber/$total")
      searchForPersonByIdentifier(record)?.let {
        when {
          it.isNotMerged() -> action(it)
          else -> log.info("Skipping $processName, item: $itemNumber/$total it has been merged")
        }
      } ?: log.info("Error $processName, record not found. id: ${record.sourceSystemId} item: $itemNumber/$total")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val UPSERT_PROCESS_NAME = "Upsert Person Records"
    private const val RECLUSTER_PROCESS_NAME = "Recluster Person Records"
  }
}
