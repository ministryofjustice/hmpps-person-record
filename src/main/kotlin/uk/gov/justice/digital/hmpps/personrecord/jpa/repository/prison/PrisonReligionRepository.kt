package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import java.util.UUID

@Repository
interface PrisonReligionRepository : JpaRepository<PrisonReligionEntity, Long> {
  fun deleteAllByPrisonNumber(prisonNumber: String)
  fun findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber: String): List<PrisonReligionEntity>

  @Query("SELECT r FROM PrisonReligionEntity r WHERE r.prisonNumber = :prisonNumber AND r.prisonRecordType = 'CURRENT'")
  fun findByPrisonNumberAndCurrentPrisonRecordType(prisonNumber: String): PrisonReligionEntity?
  fun findByUpdateId(updateId: UUID): PrisonReligionEntity?
}
