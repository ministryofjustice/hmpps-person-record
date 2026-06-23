package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.HmppsDomainEvent

@Component
class DomainEventProcessor(
  val jsonMapper: JsonMapper,
  val sqsMessageProcessor: SQSMessageProcessor,
) {

  fun process(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = sqsMessageProcessor.process(rawMessage) {
    action(jsonMapper.readValue<DomainEvent>(it.message))
  }

  final inline fun <reified T : HmppsDomainEvent> processHmppsDomainEvent(rawMessage: String, crossinline action: (domainEvent: T) -> Unit) = sqsMessageProcessor.process(rawMessage) {
    action(jsonMapper.readValue<T>(it.message))
  }
}
