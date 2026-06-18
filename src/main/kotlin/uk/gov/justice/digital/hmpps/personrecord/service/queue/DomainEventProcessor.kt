package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
@Component
class DomainEventProcessor(
  private val jsonMapper: JsonMapper,
  private val sqsMessageProcessor: SQSMessageProcessor,
) {

  fun process(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = sqsMessageProcessor.process(rawMessage) {
    action(jsonMapper.readValue<DomainEvent>(it.message))
  }
}
