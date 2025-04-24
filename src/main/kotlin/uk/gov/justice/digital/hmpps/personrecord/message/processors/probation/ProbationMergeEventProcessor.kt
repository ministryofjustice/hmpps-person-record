package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processEvent(mergeDomainEvent: DomainEvent) {
    publisher.publishEvent(
      RecordTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          EventKeys.SOURCE_CRN to mergeDomainEvent.additionalInformation?.sourceCrn,
          EventKeys.TARGET_CRN to mergeDomainEvent.additionalInformation?.targetCrn,
          EventKeys.EVENT_TYPE to mergeDomainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to DELIUS.name,
        ),
      ),
    )
    encodingService.getProbationCase(mergeDomainEvent.additionalInformation?.targetCrn!!) {
      it?.let {
        mergeService.processMerge(
          MergeEvent(
            sourceSystemId = Pair(EventKeys.SOURCE_CRN, mergeDomainEvent.additionalInformation.sourceCrn!!),
            targetSystemId = Pair(EventKeys.TARGET_CRN, mergeDomainEvent.additionalInformation.targetCrn),
            mergedRecord = Person.from(it),
            event = mergeDomainEvent.eventType,
          ),
          sourcePersonCallback = {
            personRepository.findByCrn(mergeDomainEvent.additionalInformation.sourceCrn)
          },
          targetPersonCallback = {
            personRepository.findByCrn(mergeDomainEvent.additionalInformation.targetCrn)
          },
        )
      }
    }
  }
}
