package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

@Component
class SasEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val sasClient: SasClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val reclusterService: ReclusterService,
) {

  @SqsListener(SAS_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    when (event.eventType) {
      SAS_ADDRESS_UPDATED -> {
        val sasGetAddressResponse = sasClient.getAddress(event.detailUrl!!)!!
        val crn = sasGetAddressResponse.crn
        val addressUpdateId = sasGetAddressResponse.cprAddressId
        val updatedSasAddress = sasGetAddressResponse.address

        val personEntity = personRepository.findByCrn(crn)!!
        val addressEntity = personEntity.addresses.first { it.updateId.toString() == addressUpdateId }
        addressEntity.startDate = sasGetAddressResponse.startDate
        addressEntity.endDate = sasGetAddressResponse.endDate
        addressEntity.postcode = updatedSasAddress.postcode
        addressEntity.subBuildingName = updatedSasAddress.subBuildingName
        addressEntity.buildingName = updatedSasAddress.buildingName
        addressEntity.buildingNumber = updatedSasAddress.buildingNumber
        addressEntity.thoroughfareName = updatedSasAddress.thoroughfareName
        addressEntity.dependentLocality = updatedSasAddress.dependentLocality
        addressEntity.postTown = updatedSasAddress.postTown
        addressEntity.county = updatedSasAddress.county
        addressEntity.countryCode = updatedSasAddress.country?.let { CountryCode.valueOf(it) }
        addressEntity.uprn = updatedSasAddress.uprn
        addressRepository.save(addressEntity)

        reclusterService.recluster(personEntity)
      }
      else -> throw Exception("Event of type ${event.eventType} does not have a registered processor")
    }
  }
}
