package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.SasAddress
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.time.LocalDate

@Component
class SasAddressPromotionHandler(
  private val sasClient: SasClient,
  private val personRepository: PersonRepository,
  private val addressService: AddressService,
) {

  @Transactional
  fun handle(event: DomainEvent) {
    val sasAddress = sasClient.getAddress(event.detailUrl!!)
    val personEntity = personRepository.findByCrn(sasAddress.crn)!!
    personEntity.demoteMainAddressIfPresent(sasAddress)

    addressService.processAddress(
      address = sasAddress.address,
      findPerson = { personEntity },
      findAddress = { personEntity.addresses.first { it.updateId.toString() == sasAddress.cprAddressId.toString() } },
      eventSource = DomainEventSource.CPR,
    )
  }

  private fun PersonEntity.demoteMainAddressIfPresent(sasAddress: SasAddress) {
    this.addresses
      .filter { it.updateId != sasAddress.cprAddressId }
      .firstOrNull { it.statusCode == AddressStatusCode.M }
      ?.let { mainAddressEntity ->
        mainAddressEntity.statusCode = AddressStatusCode.P
        mainAddressEntity.endDate = LocalDate.now().toUkZonedDateTime()
        addressService.processAddress(
          address = Address.from(mainAddressEntity),
          findPerson = { this },
          findAddress = { mainAddressEntity },
          eventSource = DomainEventSource.CPR,
        )
      }
  }
}
