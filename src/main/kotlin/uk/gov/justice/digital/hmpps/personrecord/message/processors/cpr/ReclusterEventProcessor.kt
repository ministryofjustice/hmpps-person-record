package uk.gov.justice.digital.hmpps.personrecord.message.processors.cpr

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED

@Component
class ReclusterEventProcessor(
  private val telemetryService: TelemetryService,
  private val reclusterService: ReclusterService,
  private val personKeyRepository: PersonKeyRepository,
  private val retryExecutor: RetryExecutor,
) {

  fun processEvent(reclusterEvent: Recluster) = runBlocking {
    telemetryService.trackEvent(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf(EventKeys.UUID to reclusterEvent.uuid.toString()),
    )
    retryExecutor.runWithRetryDatabase {
      personKeyRepository.findByPersonId(reclusterEvent.uuid)?.let { cluster ->
        val changedRecord = cluster.personEntities.first { it.id == reclusterEvent.changedRecordId }
        reclusterService.recluster(cluster = cluster, changedRecord = changedRecord)
      }
    }
  }
}
