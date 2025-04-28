package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class PrisonMergeEventProcessor(
  private val publisher: ApplicationEventPublisher,
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
  private val createUpdateService: CreateUpdateService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    publisher.publishEvent(
      RecordTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          EventKeys.FROM_SOURCE_SYSTEM_ID to domainEvent.additionalInformation?.sourcePrisonNumber,
          EventKeys.TO_SOURCE_SYSTEM_ID to domainEvent.additionalInformation?.prisonNumber,
          EventKeys.EVENT_TYPE to domainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
        ),
      ),
    )
    encodingService.getPrisonerDetails(domainEvent.additionalInformation?.prisonNumber!!) {
      it?.let {
        val from: PersonEntity? = personRepository.findByPrisonNumber(domainEvent.additionalInformation.sourcePrisonNumber!!)
        val to: PersonEntity = createUpdateService.processPerson(Person.from(it), shouldRecluster = false) { personRepository.findByPrisonNumber(domainEvent.additionalInformation.prisonNumber) }
        mergeService.processMerge(from, to)
      }
    }
  }
}
