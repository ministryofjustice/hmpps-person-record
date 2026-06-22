package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import java.time.Instant

@Component
class ProbationPersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onCreate(personCreated: PersonCreated) {
    publishPersonDomainEvent(
      personCreated.personEntity,
      CPR_PROBATION_PERSON_CREATED,
    )
  }

  private fun publishPersonDomainEvent(
    personEntity: PersonEntity,
    eventType: String,
  ) {
    val crn = personEntity.extractSourceSystemId()!!
    val sourceSystemId = personEntity.extractSourceSystemId()
    val detailUrl = "$baseUrl/person/probation/$sourceSystemId"

    domainEventPublisher.publish(
      DomainEvent(
        eventType = eventType,
        description = "A probation person record has been created",
        detailUrl = detailUrl,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
    )
  }
}
