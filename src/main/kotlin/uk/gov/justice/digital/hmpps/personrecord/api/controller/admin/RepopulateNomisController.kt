package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

@RestController
class RepopulateNomisController(
  private val personRepository: PersonRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonEventProcessor: PrisonEventProcessor,
) {

  @Hidden
  @PostMapping("/admin/repopulate-nomis")
  suspend fun postRecluster(
    @RequestBody adminReclusterRecords: List<AdminReclusterRecord>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
      triggerNomisPNC(adminReclusterRecords)
      log.info("$RECLUSTER_PROCESS_PREFIX Triggered. Number of records: ${adminReclusterRecords.size}.")
    }
  }

  fun triggerNomisPNC(adminReclusterRecords: List<AdminReclusterRecord>) {
    adminReclusterRecords.forEachPersonAndLog("Retrieving Nomis PNCs") {
      prisonerSearchClient.getPrisoner(it.prisonNumber!!)?.let { person ->
        prisonEventProcessor.processEvent(person)
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
        when {
          it.isNotMerged() -> action(it)
          else -> log.info("Skipping $processName, item: $itemNumber/$total it has been merged")
        }
      } ?: log.info("Error $processName, record not found. id: ${record.sourceSystemId} item: $itemNumber/$total")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val RECLUSTER_PROCESS_PREFIX = "ADMIN RECLUSTER: "
  }
}
