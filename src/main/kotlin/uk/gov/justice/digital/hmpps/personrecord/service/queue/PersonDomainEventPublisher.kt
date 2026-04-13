package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonDomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish

@Component
class PersonDomainEventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
) {
  private val topic =
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("Could not find topic domainevents")

  fun publish(personDomainEvent: PersonDomainEvent) {
    topic.publish(
      personDomainEvent.eventType,
      jsonMapper.writeValueAsString(personDomainEvent),
    )
  }
}
