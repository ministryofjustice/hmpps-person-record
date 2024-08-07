package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtHearingEntity

@Repository
interface CourtHearingRepository : JpaRepository<CourtHearingEntity, Long> {
  fun findByHearingId(hearingId: String?): CourtHearingEntity?
}
