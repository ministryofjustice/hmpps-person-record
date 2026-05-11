package uk.gov.justice.digital.hmpps.personrecord.message.processors.sas

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationProcessor
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient

@Component
class SasAccommodationEventProcessor(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val probationProcessor: ProbationProcessor,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(domainEvent: DomainEvent) {
    val crn = domainEvent.getCrn()
    corePersonRecordAndDeliusClient.getPerson(crn).let {
      probationProcessor.processProbationEvent(it)
    }
  }
}
