package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtMessageEntity

@Repository
interface CourtMessageRepository : JpaRepository<CourtMessageEntity, Long> {
  fun findByMessageId(messageId: String?): CourtMessageEntity
}
