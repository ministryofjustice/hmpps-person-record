package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity

@Repository
interface DefendantRepository : JpaRepository<DefendantEntity, Long> {
  fun findByDefendantId(defendantId: String): DefendantEntity ?

  fun findAllByPncNumber(pncNumber: String): List<DefendantEntity>
}
