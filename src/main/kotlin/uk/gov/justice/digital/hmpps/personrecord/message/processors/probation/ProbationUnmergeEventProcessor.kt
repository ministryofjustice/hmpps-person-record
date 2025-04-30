package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.NewUnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED

@Component
class ProbationUnmergeEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val newUnmergeService: NewUnmergeService,
  private val personRepository: PersonRepository,
  private val publisher: ApplicationEventPublisher,
  private val encodingService: EncodingService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    publisher.publishEvent(
      RecordTelemetry(
        UNMERGE_MESSAGE_RECEIVED,
        mapOf(
          EventKeys.TO_SOURCE_SYSTEM_ID to domainEvent.additionalInformation?.reactivatedCrn,
          EventKeys.FROM_SOURCE_SYSTEM_ID to domainEvent.additionalInformation?.unmergedCrn,
          EventKeys.EVENT_TYPE to domainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to DELIUS.name,
        ),
      ),
    )
    val unmergedPerson = encodingService.getProbationCase(domainEvent.additionalInformation?.unmergedCrn!!) {
      createUpdateService.processPerson(Person.from(it!!)) { personRepository.findByCrn(it.identifiers.crn!!) }
    }
    val reactivatedPerson = encodingService.getProbationCase(domainEvent.additionalInformation.reactivatedCrn!!) {
      createUpdateService.processPerson(Person.from(it!!)) { personRepository.findByCrn(it.identifiers.crn!!) }
    }
    newUnmergeService.processUnmerge(reactivatedPerson as PersonEntity, unmergedPerson as PersonEntity)
  }
}
