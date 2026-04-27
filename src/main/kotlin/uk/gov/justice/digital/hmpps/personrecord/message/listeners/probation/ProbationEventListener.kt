package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.extensions.nowUtcFormattedUk
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish

@Component
class ProbationEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
  hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  @Value($$"${core-person-record.base-url}") val baseUrl: String,
) {

  private val topic =
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("Could not find topic ")

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED -> {
        val probationAddress = corePersonRecordAndDeliusClient.getAddress(event.additionalInformation?.addressId)
        val person = personRepository.findByCrn(crn)!!

        // TODO: Once ready, make use of the new AddressService class
        val addressEntity = probationAddress?.let {
          val coreAddress = Address.from(it)
          val addressEntity = coreAddress?.let { AddressEntity.from(coreAddress) }
          addressEntity!!.person = person
          addressEntity.usages.forEach { usage -> usage.address = addressEntity }
          addressEntity.contacts.forEach { contactEntity -> contactEntity.address = addressEntity }
          addressEntity
        }!!

        addressRepository.save(addressEntity)
        topic.publish(
          "core-person-record.probation.address.created",
          jsonMapper.writeValueAsString(
            DomainEvent(
              "core-person-record.probation.address.created",
              personReference = PersonReference(listOf(PersonIdentifier(type = "CRN", value = crn))),
              additionalInformation = AdditionalInformation(cprAddressId = addressEntity.updateId.toString(), deliusAddressId = event.additionalInformation?.addressId),
              detailUrl = "$baseUrl/person/probation/$crn/address/${addressEntity.updateId}",
              description = "Address was created in Core Person Record",
              occurredAt = nowUtcFormattedUk(),
            ),
          ),
        )
      }
      else -> {
        corePersonRecordAndDeliusClient.getPerson(crn).let {
          eventProcessor.processEvent(it)
        }
      }
    }
  }
}
