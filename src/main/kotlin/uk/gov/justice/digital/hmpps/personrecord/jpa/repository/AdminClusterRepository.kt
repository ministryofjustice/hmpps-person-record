package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.admin.AdminClusterEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType

@org.springframework.stereotype.Repository
interface AdminClusterRepository : Repository<AdminClusterEntity, Long> {

  fun findAllByStatusOrderById(uuidStatus: UUIDStatusType): List<AdminClusterEntity>
}
