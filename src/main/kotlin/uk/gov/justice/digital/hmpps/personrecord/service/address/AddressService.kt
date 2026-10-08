package uk.gov.justice.digital.hmpps.personrecord.service.address

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Service
class AddressService(
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun processAddress(
    address: Address,
    findPerson: (() -> PersonEntity?)? = null,
    findAddress: () -> AddressEntity?,
    eventSource: DomainEventSource,
  ): AddressEntity = findAddress().exists(
    no = {
      create(address, findPerson?.invoke()!!, eventSource)
    },
    yes = {
      update(address, it, eventSource)
    },
  )

  private fun create(address: Address, personEntity: PersonEntity, eventSource: DomainEventSource): AddressEntity {
    val personChangeChecker = PersonChangeChecker(personEntity)
    val addressToSave = AddressEntity.from(address)
    addressToSave.person = personEntity
    personEntity.addresses.add(addressToSave)

    val addressEntity = addressRepository.save(addressToSave)

    tryRecluster(personEntity, personChangeChecker)

    publisher.publishEvent(AddressCreated(addressEntity, personChangeChecker.matchingFieldsHaveChanged(personEntity), eventSource))

    return addressEntity
  }

  private fun update(address: Address, addressEntity: AddressEntity, eventSource: DomainEventSource): AddressEntity {
    val personEntity = addressEntity.person!!
    val personChangeChecker = PersonChangeChecker(personEntity)
    addressEntity.update(address)
    addressRepository.save(addressEntity)

    tryRecluster(personEntity, personChangeChecker)

    publisher.publishEvent(AddressUpdated(addressEntity, personChangeChecker.matchingFieldsHaveChanged(personEntity), eventSource))

    return addressEntity
  }

  @Transactional
  fun deleteAddress(findAddress: () -> AddressEntity?, eventSource: DomainEventSource) {
    findAddress()?.let { addressEntity ->
      val personEntity = addressEntity.person!!
      val personChangeChecker = PersonChangeChecker(personEntity)
      personEntity.addresses.remove(addressEntity)
      addressEntity.person = null
      personRepository.save(personEntity)

      tryRecluster(personEntity, personChangeChecker)
      publisher.publishEvent(AddressDeleted(addressEntity, personEntity, eventSource))
    }
  }

  private fun tryRecluster(
    personEntity: PersonEntity,
    personChangeChecker: PersonChangeChecker,
  ) {
    if (personChangeChecker.shouldSaveToPersonMatch(personEntity)) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
  }

  private fun AddressEntity?.exists(no: () -> AddressEntity, yes: (addressEntity: AddressEntity) -> AddressEntity): AddressEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
