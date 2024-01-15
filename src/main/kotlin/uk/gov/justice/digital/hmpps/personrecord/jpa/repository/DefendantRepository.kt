package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity

@Repository
interface DefendantRepository : JpaRepository<DefendantEntity, Long> {
  fun findByDefendantId(defendantId: String): DefendantEntity?

  @Transactional(isolation = Isolation.READ_COMMITTED)
  fun findAllByPncNumber(pncNumber: String): List<DefendantEntity>
}
