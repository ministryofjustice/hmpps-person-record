package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprSysconSyncPrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprSysconSyncPrisonPersonMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.SysconSyncPrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.SYSCON_SYNC_PRISON_PERSON_MERGED

@Component
class SysconSyncPrisonPersonMergedEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onSysconSyncPrisonPersonMerged(sysconSyncPrisonPersonMerged: SysconSyncPrisonPersonMerged) = domainEventPublisher.publish(
    CprSysconSyncPrisonPersonMerged(
      eventType = SYSCON_SYNC_PRISON_PERSON_MERGED,
      description = "A prison person has been merged",
      personReferenceTo = sysconSyncPrisonPersonMerged.to.prisonPersonIdentifier(),
      additionalInformation = CprSysconSyncPrisonPersonMergedInfo(sysconSyncPrisonPersonMerged.from?.prisonPersonIdentifier()),
    ),
    attributes = mapOf("eventSource" to CPR.identifier),
  )

  private fun PersonEntity?.prisonPersonIdentifier() = when {
    this == null || prisonNumber == null -> PersonReference()
    else -> PersonReference(listOf(PersonIdentifier("prisonNumber", prisonNumber!!)))
  }
}
