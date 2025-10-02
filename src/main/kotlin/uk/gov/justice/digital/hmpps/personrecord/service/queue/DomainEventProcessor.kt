package uk.gov.justice.digital.hmpps.personrecord.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent

@Component
class DomainEventProcessor(
  private val objectMapper: ObjectMapper,
  private val sqsListenerService: SQSListenerService,
) {

  fun processDomainEvent(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = sqsListenerService.processSQSMessage(rawMessage) { action(objectMapper.readValue<DomainEvent>(it.message)) }
}
