package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreatedUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import java.util.UUID

@Component
class DeliusAddressIdHandler(private val addressRepository: AddressRepository) {
  @Transactional
  fun patchAddress(event: ProbationOffenderAddressCreatedUpdated) {
    val cprAddressUpdateId = event.additionalInformation.cprAddressId
    val existingAddressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressUpdateId))!!
    existingAddressEntity.deliusAddressId = event.additionalInformation.deliusAddressId
    addressRepository.save(existingAddressEntity)
  }
}
