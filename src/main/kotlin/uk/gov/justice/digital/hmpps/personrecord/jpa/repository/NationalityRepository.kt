package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity

interface NationalityRepository : JpaRepository<NationalityEntity, Long> {
  fun findAllByNationalityCodeIsNull(pageable: Pageable): Page<NationalityEntity>
}
