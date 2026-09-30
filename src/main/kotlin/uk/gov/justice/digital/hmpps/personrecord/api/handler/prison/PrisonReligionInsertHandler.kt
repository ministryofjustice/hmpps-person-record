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
import java.util.UUID

@Component
class PrisonReligionInsertHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun handleCprInsert(prisonNumber: String, insertRequest: PrisonReligionInsertRequest): PrisonReligionMapping {
    val prisonReligionHistory = PrisonReligionHistory(
      nomisReligionId = UUID.randomUUID().toString(),
      religionCode = insertRequest.religionCode,
      changeReasonKnown = !insertRequest.comment.isNullOrBlank(),
      comments = insertRequest.comment,
      startDate = LocalDate.now(),
      current = true,
      createDateTime = LocalDateTime.now(),
      createUserId = insertRequest.userId,
    )
    return insertHistory(prisonNumber, prisonReligionHistory, DomainEventSource.CPR)
  }

  @Transactional
  fun handleNomisInsert(prisonNumber: String, prisonReligionHistory: PrisonReligionHistory): PrisonReligionMapping = insertHistory(prisonNumber, prisonReligionHistory, DomainEventSource.NOMIS)

  private fun insertHistory(
    prisonNumber: String,
    prisonReligionHistory: PrisonReligionHistory,
    domainEventSource: DomainEventSource,
  ): PrisonReligionMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val cprReligionId = savePrisonReligion(prisonNumber, prisonReligionHistory, personEntity, domainEventSource)
    return PrisonReligionMapping(prisonReligionHistory.nomisReligionId, cprReligionId)
  }

  private fun savePrisonReligion(
    prisonNumber: String,
    prisonReligionHistory: PrisonReligionHistory,
    personEntity: PersonEntity,
    domainEventSource: DomainEventSource,
  ): String {
    prisonReligionRepository.findByPrisonNumberAndCurrentPrisonRecordType(prisonNumber)?.let { existingCurrent ->
      // duplicate existing Prison API behaviour in that end date set to today, rather than the start of the new record
      existingCurrent.endDate = LocalDate.now()
      existingCurrent.prisonRecordType = PrisonRecordType.HISTORIC
      existingCurrent.modifyUserId = prisonReligionHistory.createUserId
      existingCurrent.modifyDateTime = prisonReligionHistory.createDateTime
      prisonReligionRepository.saveAndFlush(existingCurrent)
    }
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionHistory))
    personEntity.religion = prisonReligionHistory.religionCode
    publisher.publishEvent(ReligionCreated(domainEventSource, prisonReligionEntity, SourceSystemType.NOMIS))
    personService.processPerson(Person.from(personEntity)) { personEntity }
    return prisonReligionEntity.updateId.toString()
  }
}
