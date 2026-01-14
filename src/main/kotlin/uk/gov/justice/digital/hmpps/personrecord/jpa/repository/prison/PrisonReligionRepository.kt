package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

@Repository
interface PrisonReligionRepository : JpaRepository<PrisonReligionEntity, Long> {

  @Modifying
  @Query("delete from PrisonReligionEntity pr where pr.prisonNumber = ?1")
  fun deleteInBulkByPrisonNumber(prisonNumber: String)

  fun findByPrisonNumber(prisonNumber: String): List<PrisonReligionEntity>
}
