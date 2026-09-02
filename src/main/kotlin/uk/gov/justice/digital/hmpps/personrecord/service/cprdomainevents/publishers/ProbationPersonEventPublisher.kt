package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED
import java.time.Instant

@Component
class ProbationPersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onCreate(personCreated: PersonCreated) {
    publishPersonDomainEvent(personCreated.personEntity)
  }

  override fun onUpdate(personUpdated: PersonUpdated) {
    val crn = personUpdated.personEntity.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprPersonUpdated(
        eventType = CPR_PROBATION_PERSON_UPDATED,
        description = "A probation person record has been updated",
        detailUrl = "$baseUrl/person/probation/$crn",
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
    )
  }

  override fun onDelete(personDeleted: PersonDeleted) {
    val personEntity = personDeleted.personEntity
    val crn = personEntity.extractSourceSystemId()!!

    domainEventPublisher.publish(
      domainEvent = CprPersonDeleted(
        eventType = CPR_PROBATION_PERSON_DELETED,
        occurredAt = Instant.now().asStringWithUkZone(),
        description = "A probation person record has been deleted",
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
    )
  }

  private fun publishPersonDomainEvent(personEntity: PersonEntity) {
    val crn = personEntity.extractSourceSystemId()!!

    domainEventPublisher.publish(
      CprPersonCreated(
        eventType = CPR_PROBATION_PERSON_CREATED,
        description = "A probation person record has been created",
        detailUrl = "$baseUrl/person/probation/$crn",
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
