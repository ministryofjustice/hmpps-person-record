package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import java.util.*

@Repository
interface PersonKeyRepository : JpaSpecificationExecutor<PersonKeyEntity>, JpaRepository<PersonKeyEntity, Long> {
  fun findByPersonId(personId: UUID?): PersonKeyEntity
}
