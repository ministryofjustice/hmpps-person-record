package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import java.util.*

@Repository
interface PersonIdentifierRepository : JpaSpecificationExecutor<PersonIdentifierEntity>, JpaRepository<PersonIdentifierEntity, Long> {
  fun findByPersonId(personId: UUID?): PersonIdentifierEntity
}
