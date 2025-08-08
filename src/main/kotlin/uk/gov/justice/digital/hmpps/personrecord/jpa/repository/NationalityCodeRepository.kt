package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.NationalityCodeEntity

interface NationalityCodeRepository : JpaRepository<NationalityCodeEntity, Long> {
  fun findByCode(code: String): NationalityCodeEntity?
}
