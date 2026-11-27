package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Component
class DpsPrisonGetHandler(
  private val prisonGetHelper: PrisonGetHelper,
  private val prisonReligionRepository: PrisonReligionRepository,
  private val prisonPseudonymRepository: PrisonPseudonymRepository,
) {

  @Transactional(readOnly = true)
  fun get(prisonNumber: String): ResponseEntity<DpsPrisonRecord> = prisonGetHelper.get(prisonNumber) { personEntity ->
    val prisonReligionEntities =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)

    val prisonPseudonymEntities = prisonPseudonymRepository.findAllById(personEntity.pseudonyms.mapNotNull { it.id })

    DpsPrisonRecord.from(personEntity, prisonReligionEntities, prisonPseudonymEntities)
  }
}
