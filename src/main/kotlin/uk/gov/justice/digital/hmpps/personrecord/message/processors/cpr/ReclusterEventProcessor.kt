package uk.gov.justice.digital.hmpps.personrecord.message.processors.cpr

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import java.util.UUID

@Component
class ReclusterEventProcessor(
  private val telemetryService: TelemetryService,
  private val reclusterService: ReclusterService,
  private val personKeyRepository: PersonKeyRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun processEvent(reclusterEvent: Recluster) = runBlocking {
    val personUUID = UUID.fromString(reclusterEvent.uuid)
    telemetryService.trackEvent(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf(EventKeys.UUID to reclusterEvent.uuid),
    )
    RetryExecutor.runWithRetryDatabase(retryDelay) {
      personKeyRepository.findByPersonId(personUUID)?.let {
        reclusterService.recluster(it)
      }
    }
  }
}
