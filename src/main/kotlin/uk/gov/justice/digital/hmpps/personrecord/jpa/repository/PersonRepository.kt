package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.*

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long>, PersonRepositoryCustom {
  fun findByPersonId(uuid: UUID): PersonEntity?
  fun findByOffendersCrn(crn: String): PersonEntity?
  fun findByDefendantsPncNumber(defendantsPncNumber: String): PersonEntity?
  fun findByPrisonersPncNumber(prisonerPncNumber: String): PersonEntity?
  fun findByOffendersPncNumber(offenderPncNumber: String): PersonEntity?

  @Query(
    "SELECT distinct p FROM PersonEntity p " +
      "LEFT JOIN DefendantEntity d on d.person.id = p.id " +
      "LEFT JOIN OffenderEntity o on o.person.id = p.id " +
      "LEFT JOIN PrisonerEntity p2 ON p2.person.id = p.id " +
      "WHERE o.pncNumber = :pncNumber OR d.pncNumber = :pncNumber OR p2.pncNumber = :pncNumber",
  )
  fun findPersonEntityByPncNumber(@Param("pncNumber") pncNumber: String?): PersonEntity?
}
