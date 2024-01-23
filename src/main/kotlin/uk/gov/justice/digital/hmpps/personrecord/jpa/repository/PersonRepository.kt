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
      "LEFT JOIN DefendantEntity defendant on defendant.person.id = p.id " +
      "LEFT JOIN OffenderEntity offender on offender.person.id = p.id " +
      "LEFT JOIN PrisonerEntity prisioner ON prisioner.person.id = p.id " +
      "WHERE offender.pncNumber = :pncNumber OR defendant.pncNumber = :pncNumber OR prisioner.pncNumber = :pncNumber",
  )
  fun findPersonEntityByPncNumber(@Param("pncNumber") pncNumber: String?): PersonEntity?
}
