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
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminTwin
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
class ReclusterTwinsController(
  private val transactionalReclusterService: TransactionalReclusterService,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
) {

  @Hidden
  @PostMapping("/admin/recluster-twins")
  suspend fun postRecluster(
    @RequestBody adminTwins: List<AdminTwin>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("$RECLUSTER_TWIN_PROCESS_PREFIX Triggered. Number of records: ${adminTwins.size}.")
      triggerRecluster(adminTwins)
      log.info("$RECLUSTER_TWIN_PROCESS_PREFIX Complete.")
    }
  }


  fun triggerRecluster(adminReclusterRecords: List<AdminTwin>) {
    adminReclusterRecords.forEachPersonAndLog(RECLUSTER_TWIN_PROCESS_NAME) {
      personMatchService.examineIsClusterValid(it)
    }
  }

  private fun searchForPersonByIdentifier(record: AdminTwin): PersonKeyEntity? = personKeyRepository.findByPersonUUID(record.uuid)

  private fun List<AdminTwin>.forEachPersonAndLog(processName: String, action: (PersonKeyEntity) -> Unit) {
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
      } ?: log.info("Error $processName, record not found. id: ${record.uuid} item: $itemNumber/$total")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val RECLUSTER_TWIN_PROCESS_PREFIX = "ADMIN TWIN RECLUSTER: "
    private const val RECLUSTER_TWIN_PROCESS_NAME = "Recluster Twins"
  }
}
