package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_CONTACT_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_CONTACT_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprContactUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class PrisonContactEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : ContactEventPublisher {
  override val sourceSystemType = SourceSystemType.NOMIS

  override fun onCreate(contactCreated: ContactCreated) = with(contactCreated) {
    domainEventPublisher.publish(
      CprContactCreated(
        eventType = CPR_PRISON_CONTACT_CREATED,
        description = "A prison contact has been created for a person",
        detailUrl = "$baseUrl/person/prison/$prisonNumber/contacts/${contactEntity.updateId}",
        additionalInformation = CprContactCreatedInfo(contactEntity.updateId!!),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("prisonNumber", prisonNumber))),
      ),
      attributes = mapOf("eventSource" to domainEventSource.identifier),
    )
  }

  override fun onUpdate(contactUpdated: ContactUpdated) = with(contactUpdated) {
    domainEventPublisher.publish(
      CprContactUpdated(
        eventType = CPR_PRISON_CONTACT_UPDATED,
        description = "A prison contact has been updated for a person",
        detailUrl = "$baseUrl/person/prison/$prisonNumber/contacts/${contactEntity.updateId}",
        additionalInformation = CprContactUpdatedInfo(contactEntity.updateId!!),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("prisonNumber", prisonNumber))),
      ),
      attributes = mapOf("eventSource" to domainEventSource.identifier),
    )
  }
}
