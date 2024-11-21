package uk.gov.justice.digital.hmpps.personrecord.service.person

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.ReadWriteLockService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.QueueService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val searchService: SearchService,
  private val eventLoggingService: EventLoggingService,
  private val objectMapper: ObjectMapper,
  private val queueService: QueueService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_CREATED, person)

    val sourceSystemId = extractSourceSystemId(personEntity)
    val processedDataDTO = Person.convertEntityToPerson(personEntity)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    eventLoggingService.mapToEventLogging(
      beforeData = null,
      processedData = processedData,
      sourceSystemId = sourceSystemId,
      uuid = personEntity.personKey?.personId?.toString(),
      sourceSystem = personEntity.sourceSystem.toString(),
      messageEventType = event,
    )

    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)

    val processedDataDTO = Person.convertEntityToPerson(updatedEntity)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
    updatedEntity.personKey?.personId?.let { queueService.publishReclusterMessageToQueue(it) }

    eventLoggingService.mapToEventLogging(
      beforeData = beforeData,
      processedData = processedData,
      sourceSystemId = sourceSystemId,
      uuid = existingPersonEntity.personKey?.personId?.toString(),
      sourceSystem = existingPersonEntity.sourceSystem.toString(),
      messageEventType = event,
    )

    return updatedEntity
  }

  fun linkPersonEntityToPersonKey(personEntity: PersonEntity, personKeyEntity: PersonKeyEntity?) {
    personKeyEntity?.let {
      personEntity.personKey = personKeyEntity
      personRepository.saveAndFlush(personEntity)
    }
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.from(person)
    return personRepository.saveAndFlush(personEntity)
  }

  fun searchBySourceSystem(person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsBySourceSystem(person)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }

  private fun extractSourceSystemId(personEntity: PersonEntity?): String? {
    return when (personEntity?.sourceSystem) {
      SourceSystemType.DELIUS -> personEntity.crn
      SourceSystemType.NOMIS -> personEntity.prisonNumber
      SourceSystemType.COMMON_PLATFORM -> personEntity.defendantId
      else -> null
    }
  }

}
