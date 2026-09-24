package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class SysconContactsAndAddressesMigrationHandler(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val contactRepository: ContactRepository,
  private val addressUsageRepository: AddressUsageRepository,
  private val addressRepository: AddressRepository,
) {

  @Transactional
  fun handleInsert(
    prisonNumber: String,
    prisonAddressesAndContactsRequest: PrisonAddressesAndContactsRequest,
  ): SysconAddressesAndContactsResponseBody {
    validateRequest(prisonAddressesAndContactsRequest)
    val person = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val addressMappings = handleAddressesInsert(prisonAddressesAndContactsRequest.addresses ?: emptyList(), person)
    val contactMappings = handleContactsInsert(prisonAddressesAndContactsRequest.contacts ?: emptyList(), person)

    personService.processPerson(
      person = Person.from(person),
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
    ) { person }

    return SysconAddressesAndContactsResponseBody(prisonNumber, addressMappings, contactMappings)
  }

  private fun handleContactsInsert(contacts: List<PrisonContact>, personEntity: PersonEntity): List<SysconContactMapping> {
    personEntity.contacts.clear()
    personRepository.save(personEntity)
    val contactEntities = contactRepository.saveAllAndFlush(contacts.map { it.toEntity(personEntity) })
    return contacts.zip(contactEntities).map { it.toMapping() }
  }

  private fun handleAddressesInsert(addresses: List<PrisonAddress>, personEntity: PersonEntity): List<SysconAddressMapping> {
    personEntity.addresses.clear()
    personRepository.save(personEntity)
    val mappings = addresses.map { address ->
      val addressEntity = addressRepository.saveAndFlush(address.toEntity(personEntity))

      val contactEntities = contactRepository.saveAllAndFlush(address.contacts.map { it.toEntity(addressEntity) })
      val contactMappings = address.contacts.zip(contactEntities).map { it.toMapping() }

      val addressUsageEntities = addressUsageRepository.saveAllAndFlush(address.addressUsage.map { it.toEntity(addressEntity) })
      val addressUsageMappings = address.addressUsage.zip(addressUsageEntities).map { it.toMapping() }

      SysconAddressMapping(
        nomisAddressId = address.nomisAddressId,
        cprAddressId = addressEntity.updateId.toString(),
        addressUsageMappings = addressUsageMappings,
        contactMappings = contactMappings,
      )
    }
    return mappings
  }

  private fun validateAddresses(addresses: List<PrisonAddress>?) {
    // TODO
  }

  private fun validateContacts(contacts: List<PrisonContact>?) {
    // TODO
  }

  private fun validateRequest(prisonAddressesAndContactsRequest: PrisonAddressesAndContactsRequest) {
    validateAddresses(prisonAddressesAndContactsRequest.addresses)
    validateContacts(prisonAddressesAndContactsRequest.contacts)
  }

  private fun PrisonContact.toEntity(addressEntity: AddressEntity) = ContactEntity(
    contactType = type,
    contactValue = value,
    extension = extension,
    address = addressEntity,
  )

  private fun PrisonContact.toEntity(personEntity: PersonEntity) = ContactEntity(
    contactType = type,
    contactValue = value,
    extension = extension,
    person = personEntity,
  )

  private fun PrisonAddressUsage.toEntity(addressEntity: AddressEntity) = AddressUsageEntity(
    usageCode = addressUsageCode,
    active = isActive,
    address = addressEntity,
  )

  private fun PrisonAddress.toEntity(personEntity: PersonEntity) = AddressEntity(
    startDate = startDate?.toUkZonedDateTime(),
    endDate = endDate?.toUkZonedDateTime(),
    noFixedAbode = noFixedAbode,
    fullAddress = fullAddress,
    postcode = postcode,
    subBuildingName = subBuildingName,
    buildingName = buildingName,
    buildingNumber = buildingNumber,
    thoroughfareName = thoroughfareName,
    dependentLocality = dependentLocality,
    postTown = postTown,
    county = county,
    countryCode = countryCode,
    comment = comment,
    statusCode = AddressStatusCode.fromPrison(isPrimary, isMail ?: false),
    person = personEntity,
  )

  private fun Pair<PrisonContact, ContactEntity>.toMapping() = SysconContactMapping(
    nomisContactId = first.nomisContactId,
    nomisContactType = first.type,
    cprContactId = second.updateId.toString(),
  )

  private fun Pair<PrisonAddressUsage, AddressUsageEntity>.toMapping() = SysconAddressUsageMapping(
    nomisAddressUsageId = first.nomisAddressUsageId,
    nomisAddressUsageCode = first.addressUsageCode,
    cprAddressUsageId = second.updateId.toString(),
  )
}
