package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class SysconSyncContactsHandler(
  private val personRepository: PersonRepository,
  private val contactRepository: ContactRepository,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, prisonContact: PrisonContact): SysconContactMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val contactEntity = contactRepository.saveAndFlush(prisonContact.toEntity(personEntity))
    personEntity.contacts.add(contactEntity)
    return (prisonContact to contactEntity).toMapping()
  }

  @Transactional
  fun handleUpdate(prisonNumber: String, cprContactId: String, prisonContact: PrisonContact) {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val contactEntity = personEntity.contacts.find { it.updateId.toString() == cprContactId }
      ?: throw ResourceNotFoundException("Contact with $cprContactId not found for person with $prisonNumber")
    contactEntity.updateFrom(prisonContact)
  }

  @Transactional
  fun handleDelete(prisonNumber: String, cprContactId: String) {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    personEntity.contacts.removeIf { it.updateId.toString() == cprContactId }
  }

  companion object {

    private fun Pair<PrisonContact, ContactEntity>.toMapping() = SysconContactMapping(
      nomisContactId = first.nomisContactId,
      nomisContactType = first.type,
      cprContactId = second.updateId.toString(),
    )

    fun ContactEntity.updateFrom(prisonContact: PrisonContact) {
      contactType = prisonContact.type
      contactValue = prisonContact.value
      extension = prisonContact.extension
      modifyDateTime = prisonContact.modifyDateTime
      modifyUserId = prisonContact.modifyUserId
      createDateTime = prisonContact.createDateTime
      createUserId = prisonContact.createUserId
    }

    fun PrisonContact.toEntity(personEntity: PersonEntity) = ContactEntity(
      contactType = type,
      contactValue = value,
      extension = extension,
      person = personEntity,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )
  }
}
