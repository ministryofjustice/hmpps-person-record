package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

interface PrisonReferenceRepository : JpaRepository<PrisonReferenceEntity, Long> {
  fun findAllByPseudonym(pseudonym: PseudonymEntity): List<PrisonReferenceEntity>
}
