package uk.gov.justice.digital.hmpps.personrecord.api.handler.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas.SasAddressArrivedHandler
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService

@Component
class ProbationAddressCreateHandler(
  private val personRepository: PersonRepository,
  private val addressService: AddressService,
  private val sasAddressArrivedHandler: SasAddressArrivedHandler,
) {

  @Transactional
  fun create(crn: String, address: Address): AddressEntity {
    val person = personRepository.findByCrn(crn)!!

    if (address.statusCode == AddressStatusCode.M) {
      sasAddressArrivedHandler.setMainAddressToPrevious(person, address.startDate!!)
    }
    return addressService.create(address, CPR) { person }
  }
}
