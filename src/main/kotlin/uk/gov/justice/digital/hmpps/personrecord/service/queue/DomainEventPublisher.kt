package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish

@Component
class DomainEventPublisher(
  hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
) {
  private val topic =
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("Could not find topic domainevents")

  fun publish(domainEvent: DomainEvent) {
    topic.publish(
      domainEvent.eventType,
      jsonMapper.writeValueAsString(domainEvent),
    )
  }
}
