package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.HmctsDefendantEntity

@Repository
interface HmctsDefendantRepository: JpaRepository<HmctsDefendantEntity, Long> {
  fun findByDefendantId(defendantId : String) : HmctsDefendantEntity ?
}