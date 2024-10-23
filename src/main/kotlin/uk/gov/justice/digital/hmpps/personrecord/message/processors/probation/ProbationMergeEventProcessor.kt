package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.person.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class ProbationMergeEventProcessor(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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
    encodingService.getProbationCase(domainEvent.additionalInformation?.targetCrn!!).fold(
      onSuccess = {
        it?.let {
          mergeService.processMerge(
            MergeEvent(
              sourceSystemId = Pair(EventKeys.SOURCE_CRN, domainEvent.additionalInformation.sourceCrn!!),
              targetSystemId = Pair(EventKeys.TARGET_CRN, domainEvent.additionalInformation.targetCrn),
              mergedRecord = Person.from(it),
            ),
            sourcePersonCallback = {
              personRepository.findByCrn(domainEvent.additionalInformation.sourceCrn)
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
