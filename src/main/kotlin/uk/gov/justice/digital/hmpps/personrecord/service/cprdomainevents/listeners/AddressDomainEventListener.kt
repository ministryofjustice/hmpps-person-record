package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated

@Profile("!preprod & !prod")
@Component
class AddressDomainEventListener(addressDomainEventStrategies: List<AddressEventPublisher>) {

  private val strategies = addressDomainEventStrategies.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    val sourceSystem = addressCreated.addressEntity.person!!.sourceSystem
    strategies[sourceSystem]?.onCreate(addressCreated)
  }

  @TransactionalEventListener
  fun onAddressUpdated(addressUpdated: AddressUpdated) {
    val sourceSystem = addressUpdated.addressEntity.person!!.sourceSystem
    strategies[sourceSystem]?.onUpdate(addressUpdated)
  }

  @TransactionalEventListener
  fun onAddressDeleted(addressDeleted: AddressDeleted) {
    val sourceSystem = addressDeleted.personEntity.sourceSystem
    strategies[sourceSystem]?.onDelete(addressDeleted)
  }
}
