package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ClusterNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.util.UUID

@RestController
@RequestMapping("/admin")
class AdminController(
  private val reclusterService: ReclusterService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Hidden
  @PostMapping("/recluster")
  suspend fun postRecluster(
    @RequestBody adminReclusterRequest: AdminReclusterRequest,
  ) {
    log.info("Recluster triggered, number of clusters: ${adminReclusterRequest.clusters.size}")
    CoroutineScope(Dispatchers.Default).launch {
      adminReclusterRequest.clusters.forEachIndexed { idx, uuid -> triggerRecluster(idx, uuid) }
    }
  }

  private fun triggerRecluster(idx: Int, uuid: UUID) {
    personKeyRepository.findByPersonUUID(uuid)?.let { cluster ->
      log.info("Processing cluster number: ${idx + 1}")
      publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, cluster))
      reclusterService.recluster(cluster, cluster.personEntities.first())
    } ?: throw ClusterNotFoundException(uuid)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
