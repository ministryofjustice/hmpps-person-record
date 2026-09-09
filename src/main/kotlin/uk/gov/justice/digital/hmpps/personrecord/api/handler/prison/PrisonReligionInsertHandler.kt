package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class PrisonReligionInsertHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
) {
  fun handleInsertForNomisSynchronisation(prisonNumber: String, prisonReligionHistory: PrisonReligionHistory): PrisonReligionMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val cprReligionId = handlePrisonReligionSaveForNomisSynchronisation(prisonNumber, prisonReligionHistory, personEntity)
    return PrisonReligionMapping(prisonReligionHistory.nomisReligionId, cprReligionId)
  }

  private fun handlePrisonReligionSaveForNomisSynchronisation(
    prisonNumber: String,
    prisonReligionHistory: PrisonReligionHistory,
    personEntity: PersonEntity,
  ): String {
    prisonReligionRepository.findByPrisonNumberAndCurrentPrisonRecordType(prisonNumber)?.let { existingCurrent ->
      // duplicate existing Prison API behaviour in that end date set to today, rather than the start of the new record
      existingCurrent.endDate = LocalDate.now()
      existingCurrent.prisonRecordType = PrisonRecordType.HISTORIC
      existingCurrent.modifyUserId = prisonReligionHistory.createUserId
      existingCurrent.modifyDateTime = prisonReligionHistory.createDateTime
      prisonReligionRepository.saveAndFlush(existingCurrent)
    }
    return prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionHistory)).also {
      personEntity.religion = it.code
      publisher.publishEvent(ReligionCreated(DomainEventSource.NOMIS, it, SourceSystemType.NOMIS))
      personService.processPerson(Person.from(personEntity)) { personEntity }
    }.updateId.toString()
  }

  fun handleInsert(prisonNumber: String, prisonReligionHistory: PrisonReligionInsertRequest) {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    prisonReligionRepository.findByPrisonNumberAndCurrentPrisonRecordType(prisonNumber)?.let { existingCurrent ->
      // prevent update to same value
      if (existingCurrent.code == prisonReligionHistory.religion) {
        // TODO: work out whether to throw exception here
        return
      }
      existingCurrent.endDate = LocalDate.now()
      existingCurrent.prisonRecordType = PrisonRecordType.HISTORIC
      existingCurrent.modifyUserId = prisonReligionHistory.userId
      existingCurrent.modifyDateTime = LocalDateTime.now()
      prisonReligionRepository.saveAndFlush(existingCurrent)
    }
    prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionHistory)).also {
      personEntity.religion = it.code
      publisher.publishEvent(ReligionCreated(DomainEventSource.CPR, it, SourceSystemType.NOMIS))
      personService.processPerson(Person.from(personEntity)) { personEntity }
    }
  }
}
