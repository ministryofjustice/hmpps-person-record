package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonNationalityEntity
import java.util.UUID

@Repository
interface PrisonNationalityRepository : JpaRepository<PrisonNationalityEntity, Long> {
  fun findByCprNationalityId(cprNationalityId: UUID): PrisonNationalityEntity?
}
