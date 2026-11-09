package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonRecovered
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService.PersonLookup.ByEntity

@Component
class ProbationPersonRecoveredEventProcessor(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressService: AddressService,
  private val addressRepository: AddressRepository,
  private val probationEventProcessor: ProbationEventProcessor,
) {

  @Transactional
  fun process(event: ProbationPersonRecovered) {
    corePersonRecordAndDeliusClient.getProbationCase(event.crn).let { case ->
      val personEntity = probationEventProcessor.processPerson(Person.from(case))
      case.addresses.forEach { address ->
        addressService.upsert(
          address = Address.from(address)!!,
          personLookup = ByEntity(personEntity),
          findAddress = { addressRepository.findByDeliusAddressId(address.deliusAddressId) },
          eventSource = DomainEventSource.DELIUS,
        )
      }
    }
  }
}
