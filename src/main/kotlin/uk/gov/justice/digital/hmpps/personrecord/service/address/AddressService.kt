package uk.gov.justice.digital.hmpps.personrecord.service.address

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Service
class AddressService(
  private val addressRepository: AddressRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
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

    val matchingFieldsChanged = matchingFieldsBeforeUpdate.matchingFieldsAreDifferent(personEntity)
    tryRecluster(personEntity, matchingFieldsChanged)

    publisher.publishEvent(AddressCreated(addressEntity, matchingFieldsChanged))

    return addressEntity
  }

  private fun update(address: Address, addressEntity: AddressEntity): AddressEntity {
    val matchingFieldsBeforeUpdate = PersonMatchRecord.from(addressEntity.person!!)
    addressEntity.update(address)
    addressRepository.save(addressEntity)

    val matchingFieldsChanged = matchingFieldsBeforeUpdate.matchingFieldsAreDifferent(addressEntity.person!!)
    tryRecluster(addressEntity.person!!, matchingFieldsChanged)

    publisher.publishEvent(AddressUpdated(addressEntity, matchingFieldsChanged))

    return addressEntity
  }

  private fun tryRecluster(
    personEntity: PersonEntity,
    matchingFieldsChanged: Boolean,
  ) {
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
