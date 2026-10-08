package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class SysconSyncContactsHandler(
  private val personRepository: PersonRepository,
  private val contactRepository: ContactRepository,
  private val addressRepository: AddressRepository,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, prisonContact: PrisonContact): SysconContactMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException("Person with $prisonNumber not found for person with $prisonNumber")
    val contactEntity = contactRepository.saveAndFlush(prisonContact.toEntity(personEntity))
    personEntity.contacts.add(contactEntity)
    return (prisonContact to contactEntity).toMapping()
  }

  @Transactional
  fun handleInsert(prisonNumber: String, cprAddressId: String, prisonContact: PrisonContact): SysconContactMapping {
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressId))
      ?: throw ResourceNotFoundException("Address with $cprAddressId not found for person with $prisonNumber")
    val contactEntity = contactRepository.saveAndFlush(prisonContact.toEntity(addressEntity))
    addressEntity.contacts.add(contactEntity)
    return (prisonContact to contactEntity).toMapping()
  }

  @Transactional
  fun handleUpdate(prisonNumber: String, cprContactId: String, prisonContact: PrisonContact) {
    val contactEntity = contactRepository.findByUpdateId(UUID.fromString(cprContactId))
      ?: throw ResourceNotFoundException("Contact with $cprContactId not found for person with $prisonNumber")
    contactEntity.updateFrom(prisonContact)
  }

  @Transactional
  fun handleDelete(prisonNumber: String, cprContactId: String) {
    val contactEntity = contactRepository.findByUpdateId(UUID.fromString(cprContactId))
    contactEntity?.let {
      it.person?.contacts?.remove(it)
      it.address?.contacts?.remove(it)
    }
  }

  @Transactional
  fun handleGet(prisonNumber: String, cprContactId: String): PrisonContact {
    val contactEntity = contactRepository.findByUpdateId(UUID.fromString(cprContactId))
      ?: throw ResourceNotFoundException("Contact with $cprContactId not found for person with $prisonNumber")
    return contactEntity.toDto()
  }

  companion object {

    fun Pair<PrisonContact, ContactEntity>.toMapping() = SysconContactMapping(
      nomisContactId = first.nomisContactId!!,
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

    fun ContactEntity.toDto() = PrisonContact(
      type = contactType,
      value = contactValue,
      extension = extension,
      prisonNumber = person?.prisonNumber,
      cprAddressId = address?.updateId?.toString(),
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime!!,
      createUserId = createUserId!!,
    )

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

    fun PrisonContact.toEntity(addressEntity: AddressEntity) = ContactEntity(
      contactType = type,
      contactValue = value,
      extension = extension,
      address = addressEntity,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )
  }
}
