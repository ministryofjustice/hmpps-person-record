package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

@Repository
interface PrisonPseudonymRepository : JpaRepository<PrisonPseudonymEntity, Long> {

  fun findByPseudonym(pseudonymEntity: PseudonymEntity): PrisonPseudonymEntity
}
