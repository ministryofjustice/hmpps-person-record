package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonReligionMergeHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleMerge(from: PersonEntity, to: PersonEntity) {
    val fromHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(from.prisonNumber!!)
    val toHistory: List<PrisonReligionEntity> = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(from.prisonNumber!!)

    fromHistory.onEach{it.prisonRecordType = PrisonRecordType.HISTORIC}.onEach { it.prisonNumber = from.prisonNumber!! }

  }
}
