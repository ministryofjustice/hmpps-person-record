package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.PrisonPersonReligionsMerged
import java.time.LocalDate

@Component
class PrisonReligionMergeHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun handleMerge(from: PersonEntity?, to: PersonEntity?) {
    if (from?.prisonNumber == null || to?.prisonNumber == null) {
      return
    }
    val fromHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(from.prisonNumber!!)
    val toHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(to.prisonNumber!!)

    // Choose the current religion with the latest start date and set the religion on the to person
    val currentReligions = (fromHistory + toHistory).filter { it.prisonRecordType == CURRENT }.sortedBy { it.startDate }
    if (currentReligions.size == 2) {
      val toBeHistoric = currentReligions[0]
      val current = currentReligions[1]
      toBeHistoric.prisonRecordType = HISTORIC
      toBeHistoric.endDate = LocalDate.now()
      prisonReligionRepository.saveAndFlush(toBeHistoric)
      to.religion = current.code
      personRepository.saveAndFlush(to)
    }

    // Put all of the religions on the to person
    fromHistory.forEach { it.prisonNumber = to.prisonNumber!! }
    publisher.publishEvent(PrisonPersonReligionsMerged(from, to))
  }
}
