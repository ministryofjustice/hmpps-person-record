package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPrisonPersonReligionsMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPrisonPersonReligionsMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.PrisonPersonReligionsMerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_RELIGIONS_MERGED

@Component
class ReligionsMergedEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onReligionsMerged(prisonPersonReligionsMerged: PrisonPersonReligionsMerged) = domainEventPublisher.publish(
    CprPrisonPersonReligionsMerged(
      eventType = PRISON_PERSON_RELIGIONS_MERGED,
      description = "A prison religions has been merged",
      personReferenceTo = prisonPersonReligionsMerged.to.prisonPersonIdentifier(),
      additionalInformation = CprPrisonPersonReligionsMergedInfo(prisonPersonReligionsMerged.from.prisonPersonIdentifier()),
    ),
    attributes = mapOf("eventSource" to CPR.identifier),
  )

  private fun PersonEntity?.prisonPersonIdentifier() = when {
    this == null || prisonNumber == null -> PersonReference()
    else -> PersonReference(listOf(PersonIdentifier("prisonNumber", prisonNumber!!)))
  }
}
