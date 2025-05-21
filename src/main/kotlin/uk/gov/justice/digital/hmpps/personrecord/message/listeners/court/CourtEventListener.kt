package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CourtEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService

@Component
@Profile("!seeding")
class CourtEventListener(
  private val courtEventProcessor: CourtEventProcessor,
  private val sqsListenerService: SQSListenerService,
) {

  @SqsListener(Queues.COURT_CASES_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) {
    courtEventProcessor.processEvent(it)
  }
}
