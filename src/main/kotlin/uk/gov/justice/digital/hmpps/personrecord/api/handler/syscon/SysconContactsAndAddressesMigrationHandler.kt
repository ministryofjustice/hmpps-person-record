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
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
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
    val person = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val addressesRequest = prisonAddressesAndContactsRequest.addresses ?: emptyList()
    val contactsRequest = prisonAddressesAndContactsRequest.contacts ?: emptyList()

    validateRequest(prisonNumber, addressesRequest, contactsRequest)

    val addressMappings = handleAddressesInsert(addressesRequest, person)
    val contactMappings = handleContactsInsert(contactsRequest, person)

    personService.processPerson(
      person = Person.from(person),
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
    ) { person }

    return SysconAddressesAndContactsResponseBody(prisonNumber, addressMappings, contactMappings)
  }

  private fun validateRequest(prisonNumber: String, addressesRequest: List<PrisonAddress>, contactsRequest: List<PrisonContact>) {
    validateAddresses(prisonNumber, addressesRequest)
    validateContacts(prisonNumber, personContacts = contactsRequest, addressContacts = addressesRequest.flatMap { it.contacts })
  }

  private fun validateAddresses(prisonNumber: String, addresses: List<PrisonAddress>) {
    val addressNomisDuplicateIds = addresses.groupingBy { it.nomisAddressId }.eachCount().filter { it.value > 1 }
    if (addressNomisDuplicateIds.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis address ids were detected for $prisonNumber: ${addressNomisDuplicateIds.keys.joinToString()}")
    }
    val primaryAddressIds = addresses.filter { it.isPrimary }.map { it.nomisAddressId }
    if (primaryAddressIds.size != 1) {
      throw IllegalArgumentException("There must be exactly one primary address for $prisonNumber: ${primaryAddressIds.joinToString()}")
    }
    val mailAddressIds = addresses.filter { it.isMail ?: false }.map { it.nomisAddressId }
    if (mailAddressIds.size != 1) {
      throw IllegalArgumentException("There must be exactly one mail address for $prisonNumber: ${mailAddressIds.joinToString()}")
    }
    addresses.forEach { address ->
      val addressUsageWithIncorrectIds =
        address.addressUsage.filter { it.nomisAddressUsageId != address.nomisAddressId }.map { it.nomisAddressUsageId }
      if (addressUsageWithIncorrectIds.isNotEmpty()) {
        throw IllegalArgumentException("Incorrect nomis address usage ids were detected for $prisonNumber, ${address.nomisAddressId}: ${addressUsageWithIncorrectIds.joinToString()}")
      }
      val addressUsageDuplicateCodes = address.addressUsage.groupingBy { it.addressUsageCode }.eachCount().filter { it.value > 1 }
      if (addressUsageDuplicateCodes.isNotEmpty()) {
        throw IllegalArgumentException("Duplicate nomis address usage codes were detected for $prisonNumber, ${address.nomisAddressId}: ${addressUsageDuplicateCodes.keys.joinToString()}")
      }
    }
  }

  private fun validateContacts(prisonNumber: String, personContacts: List<PrisonContact>, addressContacts: List<PrisonContact>) {
    val emailAddressContactIds = addressContacts.filter { it.type == ContactType.EMAIL }
    if (emailAddressContactIds.isNotEmpty()) {
      throw IllegalArgumentException("Email address contacts detected for: $prisonNumber, ${emailAddressContactIds.map { it.nomisContactId }.joinToString()}")
    }
    val (emailContactIds, phoneContacts) = (personContacts + addressContacts).partition { it.type == ContactType.EMAIL }
    val emailContactNomisDuplicateIds = emailContactIds.groupingBy { it.nomisContactId }.eachCount().filter { it.value > 1 }
    if (emailContactNomisDuplicateIds.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis email contact ids were detected for $prisonNumber: ${emailContactNomisDuplicateIds.keys.joinToString()}")
    }
    val phoneContactNomisDuplicateIds = phoneContacts.groupingBy { it.nomisContactId }.eachCount().filter { it.value > 1 }
    if (phoneContactNomisDuplicateIds.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis phone contact ids were detected for $prisonNumber: ${phoneContactNomisDuplicateIds.keys.joinToString()}")
    }
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
