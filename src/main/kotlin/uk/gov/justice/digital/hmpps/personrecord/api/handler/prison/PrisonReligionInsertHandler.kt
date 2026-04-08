package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonReligionInsertHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, prisonReligionHistory: PrisonReligionHistory): PrisonReligionMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val cprReligionId = handlePrisonReligionSave(prisonNumber, prisonReligionHistory, personEntity)
    return PrisonReligionMapping(prisonReligionHistory.nomisReligionId, cprReligionId)
  }

  private fun handlePrisonReligionSave(
    prisonNumber: String,
    prisonReligionHistory: PrisonReligionHistory,
    personEntity: PersonEntity,
  ): String {
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionHistory))

    personEntity.religion = prisonReligionHistory.religionCode
    personService.processPerson(Person.from(personEntity)) { personEntity }
    return prisonReligionEntity.updateId.toString()
  }
}
