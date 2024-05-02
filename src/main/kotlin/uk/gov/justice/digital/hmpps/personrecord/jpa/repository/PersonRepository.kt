package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?
}
