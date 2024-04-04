package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerEntity

@Repository
interface PrisonerRepository : JpaRepository<PrisonerEntity, Long> {

  fun findByPrisonNumber(prisonNumber: String): PrisonerEntity?
}
