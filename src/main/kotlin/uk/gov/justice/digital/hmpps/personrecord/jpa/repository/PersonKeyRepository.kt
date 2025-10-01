package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import java.util.UUID

@Repository
interface PersonKeyRepository : JpaRepository<PersonKeyEntity, Long> {
  fun findByPersonUUID(personUUID: UUID?): PersonKeyEntity?
}
