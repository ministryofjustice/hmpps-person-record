package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.*

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long>, PersonRepositoryCustom {
  fun findByPersonId(uuid: UUID): PersonEntity?
  fun findByOffendersCrn(crn: String): PersonEntity?
}
