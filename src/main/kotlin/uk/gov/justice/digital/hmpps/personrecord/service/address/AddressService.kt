package uk.gov.justice.digital.hmpps.personrecord.service.address

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Service
class AddressService(
  private val addressRepository: AddressRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
) {

  fun processAddress(
    address: Address,
    findPerson: () -> PersonEntity?,
    findAddress: () -> AddressEntity?,
  ): AddressEntity {
    val personEntity = findPerson()
      ?.takeIf { it.mergedTo == null }
      ?: throw ResourceNotFoundException("Person not found")

    return findAddress().exists(
      no = {
        create(address, personEntity)
      },
      yes = {
        update(address, it)
      },
    )
  }

  private fun create(address: Address, personEntity: PersonEntity): AddressEntity {
    val matchingFieldsBeforeUpdate = PersonMatchRecord.from(personEntity)
    val addressToSave = AddressEntity.from(address)
    addressToSave.person = personEntity
    personEntity.addresses.add(addressToSave)

    val addressEntity = addressRepository.save(addressToSave)

    reclusterIfMatchingFieldsChangedAndNotPassiveRecord(personEntity, matchingFieldsBeforeUpdate)

    return addressEntity
  }

  private fun update(address: Address, addressEntity: AddressEntity): AddressEntity {
    val matchingFieldsBeforeUpdate = PersonMatchRecord.from(addressEntity.person!!)
    addressEntity.update(address)
    addressRepository.save(addressEntity)

    reclusterIfMatchingFieldsChangedAndNotPassiveRecord(addressEntity.person!!, matchingFieldsBeforeUpdate)

    return addressEntity
  }

  private fun reclusterIfMatchingFieldsChangedAndNotPassiveRecord(
    personEntity: PersonEntity,
    matchingFieldsBeforeUpdate: PersonMatchRecord,
  ) {
    val matchingFieldsChanged = matchingFieldsBeforeUpdate.matchingFieldsAreDifferent(personEntity)
    if (matchingFieldsChanged && personEntity.isNotPassive()) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
  }

  private fun AddressEntity?.exists(no: () -> AddressEntity, yes: (addressEntity: AddressEntity) -> AddressEntity): AddressEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
