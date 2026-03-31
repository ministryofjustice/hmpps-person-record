package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord

@Component
class PrisonGetHandler(
  private val prisonerGetHelper: PrisonerGetHelper,
) {

  fun get(prisonNumber: String): ResponseEntity<CanonicalRecord> = prisonerGetHelper.get(prisonNumber) { personEntity ->
    CanonicalRecord.from(personEntity)
  }
}
