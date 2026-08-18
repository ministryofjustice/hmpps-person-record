package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonRecovered
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class ProbationPersonRecoveredEventProcessor(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressService: AddressService,
  private val addressRepository: AddressRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) {

  @Transactional
  fun process(event: ProbationPersonRecovered) {
    val probationCase = corePersonRecordAndDeliusClient.getProbationCase(event.crn)
    probationCase.let { case ->
      val personEntity = personService.processPerson(Person.from(case)) { personRepository.findByCrn(event.crn) }
      case.addresses.forEach { address ->
        addressService.processAddress(
          address = Address.from(address)!!,
          findPerson = { personEntity },
          findAddress = { addressRepository.findByDeliusAddressId(address.deliusAddressId) },
          eventSource = DomainEventSource.DELIUS,
        )
      }
    }
  }
}
