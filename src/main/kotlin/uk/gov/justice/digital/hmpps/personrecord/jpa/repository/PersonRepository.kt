package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?
  fun findAllByDefendantId(defendantId: String): List<PersonEntity>?

  fun findByCrn(crn: String): PersonEntity?
  fun findAllByCrn(crn: String): List<PersonEntity>?

  fun findByPrisonNumber(prisonNumber: String): PersonEntity?
}
