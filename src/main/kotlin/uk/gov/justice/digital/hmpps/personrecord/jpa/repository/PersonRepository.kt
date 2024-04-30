package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {

  fun existsByDefendantId(defendantId: String): Boolean

  fun findByDefendantId(defendantId: String): PersonEntity
}
