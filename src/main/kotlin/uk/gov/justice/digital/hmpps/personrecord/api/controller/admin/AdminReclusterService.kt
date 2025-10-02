package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED

@Component
class AdminReclusterService(
  private val transactionalReclusterService: TransactionalReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun recluster(person: PersonEntity) {
    person.personKey?.let { cluster ->
      publisher.publishEvent(RecordClusterTelemetry(CPR_ADMIN_RECLUSTER_TRIGGERED, cluster))
      transactionalReclusterService.triggerRecluster(person)
    }
  }
}
