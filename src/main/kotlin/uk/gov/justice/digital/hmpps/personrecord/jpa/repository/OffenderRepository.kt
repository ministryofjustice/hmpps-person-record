package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity

@Repository
interface OffenderRepository : JpaRepository<OffenderEntity, Long> {
  fun findByCrn(crn: String): OffenderEntity ?
  fun existsByCrn(crn: String): Boolean
}
