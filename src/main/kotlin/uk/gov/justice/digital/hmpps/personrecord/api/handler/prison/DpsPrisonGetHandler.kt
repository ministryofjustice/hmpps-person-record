package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Component
class DpsPrisonGetHandler(
  private val prisonGetHelper: PrisonGetHelper,
  private val prisonReligionRepository: PrisonReligionRepository,
) {

  fun get(prisonNumber: String): ResponseEntity<PrisonCanonicalRecord> = prisonGetHelper.get(prisonNumber) { personEntity ->
    val prisonReligionEntities =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)

    PrisonCanonicalRecord.from(personEntity, prisonReligionEntities)
  }
}
