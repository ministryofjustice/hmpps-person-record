package uk.gov.justice.digital.hmpps.personrecord.service.address

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import kotlin.collections.emptySet
import kotlin.reflect.KClass

@Service
class AddressService(
  private val addressRepository: AddressRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun upsert(
    address: Address,
    findPerson: () -> PersonEntity,
    findAddress: () -> AddressEntity?,
    eventSource: DomainEventSource,
    childrenToIgnore: Set<KClass<*>> = emptySet(),
  ): AddressEntity = findAddress().exists(
    no = {
      create(address, eventSource, findPerson)
    },
    yes = {
      update(address, eventSource, childrenToIgnore) { it }
    },
  )

  @Transactional
  fun create(
    address: Address,
    eventSource: DomainEventSource,
    findPerson: () -> PersonEntity,
  ): AddressEntity = personService.processUpdate(findPerson()) { personEntity, personChangeChecker ->
    val addressToSave = AddressEntity.from(address)
    addressToSave.person = personEntity
    personEntity.addresses.add(addressToSave)
    val savedAddress = addressRepository.save(addressToSave)
    publisher.publishEvent(AddressCreated(savedAddress, personChangeChecker.matchingFieldsHaveChanged(personEntity), eventSource))
    savedAddress
  }

  @Transactional
  fun update(
    address: Address,
    eventSource: DomainEventSource,
    childrenToIgnore: Set<KClass<*>> = emptySet(),
    findAddress: () -> AddressEntity?,
  ): AddressEntity {
    val addressEntity = findAddress() ?: throw ResourceNotFoundException("Address not found")
    return personService.processUpdate(addressEntity.person!!) { personEntity, personChangeChecker ->
      addressEntity.update(address, childrenToIgnore)
      val savedAddress = addressRepository.save(addressEntity)
      publisher.publishEvent(AddressUpdated(savedAddress, personChangeChecker.matchingFieldsHaveChanged(personEntity), eventSource))
      savedAddress
    }
  }

  @Transactional
  fun deleteAddress(findAddress: () -> AddressEntity?, eventSource: DomainEventSource) {
    findAddress()?.let { addressEntity ->
      personService.processUpdate(addressEntity.person!!) { personEntity, _ ->
        addressRepository.save(addressEntity)
        personEntity.addresses.remove(addressEntity)
        addressEntity.person = null
        publisher.publishEvent(AddressDeleted(addressEntity, personEntity, eventSource))
      }
    }
  }

  private fun AddressEntity?.exists(no: () -> AddressEntity, yes: (addressEntity: AddressEntity) -> AddressEntity): AddressEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
