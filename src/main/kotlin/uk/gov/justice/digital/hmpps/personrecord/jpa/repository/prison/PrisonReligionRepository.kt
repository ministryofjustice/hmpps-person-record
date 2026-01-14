package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

@Repository
interface PrisonReligionRepository : JpaRepository<PrisonReligionEntity, Long> {

  fun deleteByPrisonNumber(prisonNumber: String)

  fun findByPrisonNumber(prisonNumber: String): List<PrisonReligionEntity>
  fun findAllByPrisonNumber(prisonNumber: String): MutableList<PrisonReligionEntity>
}
