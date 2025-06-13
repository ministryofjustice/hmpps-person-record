package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ClusterNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
@RequestMapping("/admin")
class AdminController(
  private val reclusterService: ReclusterService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Hidden
  @PostMapping("/recluster")
  fun postRecluster(
    @RequestBody adminReclusterRequest: AdminReclusterRequest,
  ) {
    adminReclusterRequest.clusters.forEach {
      personKeyRepository.findByPersonUUID(it)?.let { cluster ->
        publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, cluster))
        reclusterService.recluster(cluster, cluster.personEntities.first())
      } ?: throw ClusterNotFoundException(it)
    }
  }
}
