package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED

@Component
@Profile("!seeding")
class ProbationEventListener(
  private val sqsListenerService: SQSListenerService,
  private val eventProcessor: ProbationEventProcessor,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) {
    when (it.getEventType()) {
      OFFENDER_ALIAS_CHANGED -> handleAliasUpdate(it)
      else -> handleDomainEvent(it)
    }
  }

  private fun handleDomainEvent(sqsMessage: SQSMessage) {
    val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }!!.value
    processEvent(crn)
  }

  private fun handleAliasUpdate(sqsMessage: SQSMessage) {
    val probationEvent = objectMapper.readValue<ProbationEvent>(sqsMessage.message)
    processEvent(probationEvent.crn)
  }

  private fun processEvent(crn: String) {
    try {
      eventProcessor.processEvent(crn)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    }
  }
}
