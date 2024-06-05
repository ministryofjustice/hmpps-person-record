package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Repository
interface PersonRepository : JpaSpecificationExecutor<PersonEntity>, JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?

  fun findByCrn(crn: String): PersonEntity?

  fun findByPrisonNumber(prisonNumber: String): PersonEntity?

  fun findByFirstName(firstName: String): PersonEntity?
}
