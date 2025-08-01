package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.TitleCodeEntity
import java.util.*

@Repository
interface TitleCodeRepository : JpaRepository<TitleCodeEntity, Long> {
  fun findByCode(code: String): TitleCodeEntity?
}
