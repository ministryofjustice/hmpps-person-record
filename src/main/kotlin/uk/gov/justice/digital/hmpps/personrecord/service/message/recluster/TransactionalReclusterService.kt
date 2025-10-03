package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class TransactionalReclusterService(
  private val retryableReclusterService: RetryableReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun recluster(person: PersonEntity) {
    person.personKey?.let { cluster ->
      publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, cluster))
      retryableReclusterService.triggerRecluster(person)
    }
  }
}
