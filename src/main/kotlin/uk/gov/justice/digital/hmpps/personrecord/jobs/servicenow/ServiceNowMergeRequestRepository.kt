package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ServiceNowMergeRequestRepository : JpaRepository<ServiceNowMergeRequestEntity, Long> {
  fun existsByPersonUUID(personUUID: UUID): Boolean
}
