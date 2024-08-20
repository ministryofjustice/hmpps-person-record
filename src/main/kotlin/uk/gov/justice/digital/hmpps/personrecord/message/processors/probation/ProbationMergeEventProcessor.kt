package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class ProbationMergeEventProcessor(
  val telemetryService: TelemetryService,
  val personRepository: PersonRepository,
  val mergeService: MergeService,
  @Value("\${retry.delay}")
  val retryDelay: Long = 0,
  corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) : BaseProbationEventProcessor(corePersonRecordAndDeliusClient) {

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.SOURCE_CRN to domainEvent.additionalInformation?.sourceCrn,
        EventKeys.TARGET_CRN to domainEvent.additionalInformation?.targetCrn,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )
    getProbationCase(domainEvent.additionalInformation?.targetCrn!!, retryDelay).fold(
      onSuccess = {
        it?.let {
          mergeService.processMerge(
            Person.from(it),
            sourcePersonCallback = {
              personRepository.findByCrn(domainEvent.additionalInformation.sourceCrn!!)
            },
            targetPersonCallback = {
              personRepository.findByCrn(domainEvent.additionalInformation.targetCrn)
            },
          )
        }
      },
      onFailure = {
        log.error("Error retrieving offender detail: ${it.message}")
        throw it
      },
    )
  }
}
