package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationDeleteProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService.Companion.whenEvent
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION

@Component
@Profile("!seeding")
class ProbationDeletionEventListener(
  private val sqsListenerService: SQSListenerService,
  private val probationDeleteProcessor: ProbationDeleteProcessor,
) {

  @SqsListener(Queues.PROBATION_DELETION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processDomainEvent(rawMessage) { domainEvent ->
    domainEvent.whenEvent(OFFENDER_GDPR_DELETION) { probationDeleteProcessor.processEvent(it) }
  }
}
