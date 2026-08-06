package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonReligionMergeHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleMerge(from: PersonEntity?, to: PersonEntity) {
    if (from?.prisonNumber == null || to.prisonNumber == null) {
      return
    }
    val fromHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(from.prisonNumber!!)
    val toHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(to.prisonNumber!!)

    // Choose the current religion with the latest start date and set the religion on the from person
    val currentReligions = (fromHistory + toHistory).filter { it.prisonRecordType == CURRENT }
    if (currentReligions.size > 1) {
      val toBeHistoric = currentReligions.minBy { it.startDate }
      val current = currentReligions.maxBy { it.startDate }
      toBeHistoric.prisonRecordType = HISTORIC
      toBeHistoric.endDate = current.startDate
      prisonReligionRepository.saveAndFlush(toBeHistoric)
      to.religion = current.code
      personService.processPerson(Person.from(to)) { to }
    }

    // Put all of the religions on the to person
    fromHistory.forEach { it.prisonNumber = to.prisonNumber!! }
  }
}
