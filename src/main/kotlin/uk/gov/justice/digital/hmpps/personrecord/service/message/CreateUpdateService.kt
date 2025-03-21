package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.shouldCreateOrUpdate
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.QueueService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST

@Component
class CreateUpdateService(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val queueService: QueueService,
  private val eventLoggingService: EventLoggingService,
) {

  @Transactional
  fun processPerson(person: Person, event: String?, callback: () -> PersonEntity?) {
    val existingPersonEntitySearch: PersonEntity? = callback()
    existingPersonEntitySearch.shouldCreateOrUpdate(
      shouldCreate = {
        handlePersonCreation(person, event)
      },
      shouldUpdate = {
        handlePersonUpdate(person, it, event)
      },
    )
  }

  private fun handlePersonCreation(person: Person, event: String?): PersonEntity {
    if (isUpdateEvent(event)) {
      telemetryService.trackPersonEvent(CPR_UPDATE_RECORD_DOES_NOT_EXIST, person)
    }
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    personService.linkRecordToPersonKey(personEntity)

    val processedDataDTO = Person.from(personEntity)

    eventLoggingService.recordEventLog(
      beforePerson = null,
      processedPerson = processedDataDTO,
      uuid = personEntity.personKey?.personId?.toString(),
      eventType = event,
    )

    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?): PersonEntity {
    if (isCreateEvent(event)) {
      telemetryService.trackPersonEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    val beforeDataDTO = Person.from(existingPersonEntity)
    val updatedPerson = personService.updatePersonEntity(person, existingPersonEntity)
    val processedDataDTO = Person.from(updatedPerson)

    eventLoggingService.recordEventLog(
      beforePerson = beforeDataDTO,
      processedPerson = processedDataDTO,
      uuid = existingPersonEntity.personKey?.personId?.toString(),
      eventType = event,
    )
    updatedPerson.personKey?.personId?.let { queueService.publishReclusterMessageToQueue(it) }
    return updatedPerson
  }

  private fun isUpdateEvent(event: String?) = listOf(
    PRISONER_UPDATED,
    OFFENDER_DETAILS_CHANGED,
    OFFENDER_ALIAS_CHANGED,
    OFFENDER_ADDRESS_CHANGED,
  ).contains(event)

  private fun isCreateEvent(event: String?) = listOf(PRISONER_CREATED, NEW_OFFENDER_CREATED).contains(event)
}
