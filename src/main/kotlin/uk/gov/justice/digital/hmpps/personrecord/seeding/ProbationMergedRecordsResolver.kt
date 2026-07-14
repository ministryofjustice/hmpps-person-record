package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED
import uk.gov.justice.digital.hmpps.personrecord.service.message.PersonDeletionService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class ProbationMergedRecordsResolver(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val addressService: AddressService,
  private val addressRepository: AddressRepository,
  private val publisher: ApplicationEventPublisher,
  private val personDeletionService: PersonDeletionService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  @Transactional
  fun resolve(resolveMergedRecordsConfig: ResolveMergedRecordsConfig) {
    personDeletionService.processDelete {
      personRepository.findByCrn(resolveMergedRecordsConfig.crnToDelete)
    }
    log.info("Deleted person with crn ${resolveMergedRecordsConfig.crnToDelete} and any merged records")

    corePersonRecordAndDeliusClient.getProbationCaseOnly(resolveMergedRecordsConfig.crnToRecreate).let { case ->
      val person = Person.from(case)
      val personEntity = personService.processPerson(person) { personRepository.findByCrn(person.crn!!) }
      case.addresses.forEach { address ->
        addressService.processAddress(
          address = Address.from(address)!!,
          findPerson = { personEntity },
          findAddress = { addressRepository.findByDeliusAddressId(address.deliusAddressId) },
          eventSource = DomainEventSource.DELIUS,
        )
      }
      publisher.publishEvent(RecordEventLog(CPR_RECORD_SEEDED, personRepository.findByCrn(resolveMergedRecordsConfig.crnToRecreate)!!))
    }
    log.info("Recreated person with crn ${resolveMergedRecordsConfig.crnToRecreate}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
