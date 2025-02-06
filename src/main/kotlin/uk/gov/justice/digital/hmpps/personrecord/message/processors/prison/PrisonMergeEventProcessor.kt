package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_PRISON_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.TARGET_PRISON_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class PrisonMergeEventProcessor(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        SOURCE_PRISON_NUMBER to domainEvent.additionalInformation?.sourcePrisonNumber,
        TARGET_PRISON_NUMBER to domainEvent.additionalInformation?.prisonNumber,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
      ),
    )
    val prisonNumber = domainEvent.additionalInformation!!.prisonNumber!!
    encodingService.getPrisonerDetails(
      prisonNumber,
      onSuccess = withPrisoner(domainEvent.additionalInformation, domainEvent),
    )
  }

  private fun withPrisoner(additionalInformation: AdditionalInformation, domainEvent: DomainEvent): (value: Prisoner?) -> Unit? = {
    it?.let {
      mergeService.processMerge(
        MergeEvent(
          sourceSystemId = Pair(SOURCE_PRISON_NUMBER, additionalInformation.sourcePrisonNumber!!),
          targetSystemId = Pair(TARGET_PRISON_NUMBER, additionalInformation.prisonNumber!!),
          mergedRecord = Person.from(it),
          event = domainEvent.eventType,
        ),
        sourcePersonCallback = {
          personRepository.findByPrisonNumber(additionalInformation.sourcePrisonNumber)
        },
        targetPersonCallback = {
          personRepository.findByPrisonNumber(additionalInformation.prisonNumber)
        },
      )
    }
  }
}
