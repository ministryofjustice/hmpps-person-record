package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonRecovered
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService

@Component
class ProbationPersonRecoveredEventProcessor(
  private val probationProcessor: ProbationProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressService: AddressService,
) {

  @Transactional
  fun process(event: ProbationPersonRecovered) {
    corePersonRecordAndDeliusClient.getPerson(event.crn).let {
      // recreate the person record
      val personEntity = probationProcessor.processProbationEvent(it)

      // recreate the addresses
      corePersonRecordAndDeliusClient.getAddresses(event.crn).forEach { address ->
        addressService.processAddress(
          address = address,
          findPerson = { personEntity },
          findAddress = { null },
          eventSource = DomainEventSource.DELIUS,
        )
      }
    }
  }
}
