package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent

@Component
class DomainEventProcessor(
  private val jsonMapper: JsonMapper,
  private val sqsListenerService: SQSListenerService,
) {

  fun processDomainEvent(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = sqsListenerService.processSQSMessage(rawMessage) {
    action(jsonMapper.readValue<DomainEvent>(it.message, DomainEvent::class.java))
  }
}
