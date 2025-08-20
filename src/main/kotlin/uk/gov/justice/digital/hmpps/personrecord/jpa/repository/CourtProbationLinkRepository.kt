package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtProbationLinkEntity

interface CourtProbationLinkRepository : JpaRepository<CourtProbationLinkEntity, Long> {
  fun findByDefendantId(defendantId: String): CourtProbationLinkEntity?
  fun findByCrn(crn: String): CourtProbationLinkEntity?
}
