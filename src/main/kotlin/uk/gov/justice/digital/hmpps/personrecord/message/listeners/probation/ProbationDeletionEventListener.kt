package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationDeleteProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION

@Component
@Profile("!seeding")
class ProbationDeletionEventListener(
  private val probationDeleteProcessor: ProbationDeleteProcessor,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener(Queues.PROBATION_DELETION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> {
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        when (sqsMessage.messageAttributes?.eventType?.value) {
          OFFENDER_GDPR_DELETION -> handleDeleteEvent(domainEvent)
        }
      }
    }
  }

  private fun handleDeleteEvent(domainEvent: DomainEvent) {
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }!!.value
    probationDeleteProcessor.processEvent(crn, domainEvent.eventType)
  }

}
