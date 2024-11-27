package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.person.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class PrisonMergeEventProcessor(
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
        EventKeys.SOURCE_PRISON_NUMBER to domainEvent.additionalInformation?.sourcePrisonNumber,
        EventKeys.TARGET_PRISON_NUMBER to domainEvent.additionalInformation?.prisonNumber,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
      ),
    )
    encodingService.getPrisonerDetails(domainEvent.additionalInformation?.prisonNumber!!).fold(
      onSuccess = {
        it?.let {
          mergeService.processMerge(
            MergeEvent(
              sourceSystemId = Pair(EventKeys.SOURCE_PRISON_NUMBER, domainEvent.additionalInformation.sourcePrisonNumber!!),
              targetSystemId = Pair(EventKeys.TARGET_PRISON_NUMBER, domainEvent.additionalInformation.prisonNumber),
              mergedRecord = Person.from(it),
              event = domainEvent.eventType,
            ),
            sourcePersonCallback = {
              personRepository.findByPrisonNumberAndSourceSystem(domainEvent.additionalInformation.sourcePrisonNumber)
            },
            targetPersonCallback = {
              personRepository.findByPrisonNumberAndSourceSystem(domainEvent.additionalInformation.prisonNumber)
            },
          )
        }
      },
      onFailure = {
        log.error("Error retrieving prisoner detail: ${it.message}")
        throw it
      },
    )
  }
}
