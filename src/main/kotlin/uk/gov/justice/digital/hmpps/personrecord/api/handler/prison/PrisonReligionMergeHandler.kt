package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import java.time.LocalDate

@Profile("!prod")
@Component
class PrisonReligionMergeHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
) {

  @Transactional
  fun handleMerge(from: PersonEntity?, to: PersonEntity?) {
    if (from?.prisonNumber == null || to?.prisonNumber == null) {
      return
    }
    val fromHistory: List<PrisonReligionEntity> =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(from.prisonNumber!!)
    val toHistory: List<PrisonReligionEntity> =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(to.prisonNumber!!)

    // Choose the current religion with the latest start date
    val currentReligions =
      (fromHistory + toHistory).filter { it.endDate == null || it.prisonRecordType == CURRENT }
        .sortedWith(compareByDescending<PrisonReligionEntity> { it.startDate }.thenByDescending { it.createDateTime })
    val currentReligion = currentReligions.firstOrNull()
    if (currentReligions.size > 1) {
      currentReligions.drop(1).forEach { toBeHistoric ->
        toBeHistoric.prisonRecordType = HISTORIC
        if (toBeHistoric.endDate == null) {
          toBeHistoric.endDate = LocalDate.now()
        }
        prisonReligionRepository.saveAndFlush(toBeHistoric)
      }
    }

    // Put all of the religions on the to person
    prisonReligionRepository.saveAllAndFlush(fromHistory.onEach { it.prisonNumber = to.prisonNumber!! })

    // Set the current religion on the to person and make sure its current
    currentReligion?.let {
      to.religion = it.code
      personRepository.saveAndFlush(to)
      it.prisonRecordType = CURRENT
      prisonReligionRepository.saveAndFlush(it)
    }
  }
}
