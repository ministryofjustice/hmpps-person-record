package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import java.util.UUID

@Component
class DeliusAddressIdHandler(private val addressRepository: AddressRepository) {
  @Transactional
  fun patchAddress(event: DomainEvent) {
    val cprAddressUpdateId = event.additionalInformation!!.inboundCprAddressId!!
    val existingAddressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressUpdateId))!!
    existingAddressEntity.deliusAddressId = event.additionalInformation.inboundDeliusAddressId!!
    addressRepository.save(existingAddressEntity)
  }
}
