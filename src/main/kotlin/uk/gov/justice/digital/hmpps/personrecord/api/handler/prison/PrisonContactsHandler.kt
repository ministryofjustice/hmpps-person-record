package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactUpdated
import java.time.LocalDateTime
import java.util.UUID

@Component
class PrisonContactsHandler(
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val contactRepository: ContactRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional(readOnly = true)
  fun get(prisonNumber: String, includeTypes: List<ContactType>?, excludeTypes: List<ContactType>?): List<CanonicalContact> = findPerson(prisonNumber).contacts
    .filter { includeTypes.isNullOrEmpty() || it.contactType in includeTypes }
    .filterNot { excludeTypes?.contains(it.contactType) == true }
    .map { CanonicalContact.from(it) }

  @Transactional
  fun create(prisonNumber: String, request: PrisonContactRequest): CanonicalContact {
    val personEntity = findPerson(prisonNumber)
    val contactEntity = contactRepository.saveAndFlush(
      ContactEntity(
        contactType = request.type,
        contactValue = request.value,
        extension = request.extension,
        person = personEntity,
        createDateTime = LocalDateTime.now(),
        createUserId = request.userId,
      ),
    )
    personEntity.contacts.add(contactEntity)
    publisher.publishEvent(ContactCreated(DomainEventSource.CPR, prisonNumber, contactEntity, SourceSystemType.NOMIS))
    return CanonicalContact.from(contactEntity)
  }

  @Transactional
  fun update(prisonNumber: String, contactId: String, request: PrisonContactRequest): CanonicalContact = findPerson(prisonNumber).contacts.firstOrNull { it.updateId.toString() == contactId }?.let {
    update(prisonNumber, it, request)
  }
    ?: throw ResourceNotFoundException("Contact with $contactId not found for person with $prisonNumber")

  @Transactional
  fun createContacts(prisonNumber: String, addressId: String, requests: List<PrisonContactRequest>): List<CanonicalContact> {
    val addressEntity = findAddress(addressId)
    val contactEntities = contactRepository.saveAllAndFlush(
      requests.map {
        ContactEntity(
          contactType = it.type,
          contactValue = it.value,
          extension = it.extension,
          address = addressEntity,
          createDateTime = LocalDateTime.now(),
          createUserId = it.userId,
        )
      },
    )
    addressEntity.contacts.addAll(contactEntities)
    contactEntities.forEach {
      publisher.publishEvent(ContactCreated(DomainEventSource.CPR, prisonNumber, it, SourceSystemType.NOMIS))
    }
    return contactEntities.map { CanonicalContact.from(it) }
  }

  private fun findPerson(prisonNumber: String): PersonEntity = personRepository.findByPrisonNumber(prisonNumber)
    ?: throw ResourceNotFoundException("Person with $prisonNumber not found")

  private fun findAddress(addressId: String): AddressEntity = addressRepository.findByUpdateId(
    UUID.fromString(addressId),
  )
    ?: throw ResourceNotFoundException("Address with $addressId not found")

  @Transactional
  fun updateContact(prisonNumber: String, addressId: String, contactId: String, request: PrisonContactRequest): CanonicalContact = findAddress(addressId).contacts.firstOrNull { it.updateId.toString() == contactId }?.let {
    update(prisonNumber, it, request)
  }
    ?: throw ResourceNotFoundException("Contact with $contactId not found for address with $addressId")

  private fun update(prisonNumber: String, contactEntity: ContactEntity, request: PrisonContactRequest): CanonicalContact = with(request) {
    contactEntity.contactType = type
    contactEntity.contactValue = value
    contactEntity.extension = extension
    contactEntity.modifyDateTime = LocalDateTime.now()
    contactEntity.modifyUserId = userId
  }.also {
    publisher.publishEvent(ContactUpdated(DomainEventSource.CPR, prisonNumber, contactEntity, SourceSystemType.NOMIS))
  }.let { CanonicalContact.from(contactEntity) }
}
