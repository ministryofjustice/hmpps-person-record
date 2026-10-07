package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncContactsHandler.Companion.toEntity
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncContactsHandler.Companion.toMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class SysconContactsAndAddressesMigrationHandler(
  private val personRepository: PersonRepository,
  private val contactRepository: ContactRepository,
  private val addressUsageRepository: AddressUsageRepository,
  private val addressRepository: AddressRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
) {

  @Transactional
  fun handleInsert(
    prisonNumber: String,
    prisonAddressesAndContactsRequest: PrisonAddressesAndContactsRequest,
  ): SysconAddressesAndContactsResponseBody {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val matchingFieldsBeforeUpdate = PersonMatchRecord.from(personEntity)

    val addressesRequest = prisonAddressesAndContactsRequest.addresses
    val contactsRequest = prisonAddressesAndContactsRequest.contacts

    validateRequest(prisonNumber, addressesRequest, contactsRequest)

    val addressMappings = handleAddressesInsert(addressesRequest, personEntity)
    val contactMappings = handleContactsInsert(contactsRequest, personEntity)

    val matchingFieldsChanged = matchingFieldsBeforeUpdate.matchingFieldsAreDifferent(personEntity)
    tryRecluster(personEntity, matchingFieldsChanged)

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
    if (primaryAddressIds.size > 1) {
      throw IllegalArgumentException("There cannot be more than one primary address for $prisonNumber: ${primaryAddressIds.joinToString()}")
    }
    val mailAddressIds = addresses.filter { it.isMail ?: false }.map { it.nomisAddressId }
    if (mailAddressIds.size > 1) {
      throw IllegalArgumentException("There cannot be more than one mail address for $prisonNumber: ${mailAddressIds.joinToString()}")
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
    val contactEntities = contactRepository.saveAllAndFlush(contacts.map { it.toEntity(personEntity) })
    personEntity.contacts.addAll(contactEntities)
    return contacts.zip(contactEntities).map { it.toMapping() }
  }

  private fun handleAddressesInsert(addresses: List<PrisonAddress>, personEntity: PersonEntity): List<SysconAddressMapping> {
    personEntity.addresses.clear()
    val mappings = addresses.map { prisonAddress ->
      val addressEntity = addressRepository.save(prisonAddress.toAddress(personEntity))
      personEntity.addresses.add(addressEntity)

      val contactEntities = contactRepository.saveAllAndFlush(prisonAddress.contacts.map { it.toEntity(addressEntity) })
      val contactMappings = prisonAddress.contacts.zip(contactEntities).map { it.toMapping() }
      addressEntity.contacts.addAll(contactEntities)

      val addressUsageEntities = addressUsageRepository.saveAllAndFlush(prisonAddress.addressUsage.map { it.toEntity(addressEntity) })
      val addressUsageMappings = prisonAddress.addressUsage.zip(addressUsageEntities).map { it.toMapping() }
      addressEntity.usages.addAll(addressUsageEntities)

      SysconAddressMapping(
        nomisAddressId = prisonAddress.nomisAddressId!!,
        cprAddressId = addressEntity.updateId.toString(),
        addressUsageMappings = addressUsageMappings,
        contactMappings = contactMappings,
      )
    }
    return mappings
  }

  private fun Pair<PrisonAddressUsage, AddressUsageEntity>.toMapping() = SysconAddressUsageMapping(
    nomisAddressUsageId = first.nomisAddressUsageId,
    nomisAddressUsageCode = first.addressUsageCode,
    cprAddressUsageId = second.updateId.toString(),
  )

  private fun tryRecluster(
    personEntity: PersonEntity,
    matchingFieldsChanged: Boolean,
  ) {
    if (matchingFieldsChanged && personEntity.isNotPassive()) {
      personMatchService.saveToPersonMatch(personEntity)
      reclusterService.recluster(personEntity)
    }
  }

  companion object {

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

    fun PrisonAddressUsage.toEntity(addressEntity: AddressEntity) = AddressUsageEntity(
      usageCode = addressUsageCode,
      active = isActive,
      address = addressEntity,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )

    fun PrisonAddress.toAddress(personEntity: PersonEntity) = AddressEntity(
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
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )
  }
}
