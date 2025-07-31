package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

interface PseudonymRepository: JpaRepository<PseudonymEntity, Long> {
  fun getAllByTitleNotNull(pageable: Pageable): Page<PseudonymEntity>
}