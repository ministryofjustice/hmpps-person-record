package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity

@Repository
interface EthnicityCodeRepository : JpaRepository<EthnicityCodeEntity, Long> {
  fun findByCode(code: String): EthnicityCodeEntity?
}
