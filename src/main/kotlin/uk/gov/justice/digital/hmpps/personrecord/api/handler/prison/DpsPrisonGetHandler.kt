package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Component
class DpsPrisonGetHandler(
  private val prisonGetHelper: PrisonGetHelper,
  private val personRepository: PersonRepository,
  private val prisonReligionRepository: PrisonReligionRepository,
) {

  fun get(prisonNumber: String): ResponseEntity<DpsPrisonRecord> = prisonGetHelper.get(prisonNumber) { personEntity ->
    val prisonReligionEntities =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)

    DpsPrisonRecord.from(personEntity, prisonReligionEntities)
  }

  fun getReligionHistory(prisonNumber: String): List<PrisonReligion> {
    personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)
    return prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
      .map { PrisonReligion.from(it) }
  }
}
