package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class ProbationMergeEventProcessor(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
) {

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
    encodingService.getProbationCase(domainEvent.additionalInformation?.targetCrn!!) {
      it?.let {
        mergeService.processMerge(
          MergeEvent(
            sourceSystemId = Pair(EventKeys.SOURCE_CRN, domainEvent.additionalInformation.sourceCrn!!),
            targetSystemId = Pair(EventKeys.TARGET_CRN, domainEvent.additionalInformation.targetCrn),
            mergedRecord = Person.from(it),
            event = domainEvent.eventType,
          ),
          sourcePersonCallback = {
            personRepository.findByCrn(domainEvent.additionalInformation.sourceCrn)
          },
          targetPersonCallback = {
            personRepository.findByCrn(domainEvent.additionalInformation.targetCrn)
          },
        )
      }
    }
  }
}
