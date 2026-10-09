package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContactType
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactUpdated
import java.time.LocalDateTime

@Component
class PrisonContactsHandler(
  private val personRepository: PersonRepository,
  private val contactRepository: ContactRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional(readOnly = true)
  fun get(prisonNumber: String, includeTypes: List<ContactType>?, excludeTypes: List<ContactType>?): List<PrisonContactResponse> = findPerson(prisonNumber).contacts
    .filter { includeTypes.isNullOrEmpty() || it.contactType in includeTypes }
    .filterNot { excludeTypes?.contains(it.contactType) == true }
    .map { it.toPrisonContactResponse() }

  @Transactional
  fun create(prisonNumber: String, request: PrisonContactRequest): PrisonContactResponse {
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
    return contactEntity.toPrisonContactResponse()
  }

  @Transactional
  fun update(prisonNumber: String, contactId: String, request: PrisonContactRequest): PrisonContactResponse {
    val contactEntity = findPerson(prisonNumber).contacts.firstOrNull { it.updateId.toString() == contactId }
      ?: throw ResourceNotFoundException("Contact with $contactId not found for person with $prisonNumber")
    contactEntity.contactType = request.type
    contactEntity.contactValue = request.value
    contactEntity.extension = request.extension
    contactEntity.modifyDateTime = LocalDateTime.now()
    contactEntity.modifyUserId = request.userId
    val savedContactEntity = contactRepository.saveAndFlush(contactEntity)
    publisher.publishEvent(ContactUpdated(DomainEventSource.CPR, prisonNumber, savedContactEntity, SourceSystemType.NOMIS))
    return savedContactEntity.toPrisonContactResponse()
  }

  private fun findPerson(prisonNumber: String): PersonEntity = personRepository.findByPrisonNumber(prisonNumber)
    ?: throw ResourceNotFoundException("Person with $prisonNumber not found")

  private fun ContactEntity.toPrisonContactResponse() = PrisonContactResponse(
    contactId = updateId.toString(),
    type = CanonicalContactType.from(contactType),
    value = contactValue.orEmpty(),
    extension = extension,
    createDateTime = createDateTime,
    createUserId = createUserId,
    modifyDateTime = modifyDateTime,
    modifyUserId = modifyUserId,
  )
}
