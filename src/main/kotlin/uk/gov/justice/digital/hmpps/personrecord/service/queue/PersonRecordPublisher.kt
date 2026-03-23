package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PERSON_CREATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PERSON_DELETED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PERSON_UPDATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonDomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish

@Component
class PersonRecordPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  @Value("\${core-person-record.base-url}") private val baseUrl: String,
) {
  private val topic =
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("Could not find topic domainevents")

  fun publishPersonUpdated(personEntity: PersonEntity) {
    val event = PersonDomainEvent.from(personEntity, baseUrl)
    val payload = jsonMapper.writeValueAsString(event)
    topic.publish(
      eventType = PERSON_UPDATED_EVENT_TYPE,
      event = payload,
    )
  }

  fun publishPersonCreated(personEntity: PersonEntity) {
    val event = PersonDomainEvent.from(personEntity, baseUrl)
    val payload = jsonMapper.writeValueAsString(event)
    topic.publish(
      eventType = PERSON_CREATED_EVENT_TYPE,
      event = payload,
    )
  }
  fun publishPersonDeleted(personEntity: PersonEntity) {
    val event = PersonDomainEvent.from(personEntity, baseUrl)
    val payload = jsonMapper.writeValueAsString(event)
    topic.publish(
      eventType = PERSON_DELETED_EVENT_TYPE,
      event = payload,
    )
  }
}
