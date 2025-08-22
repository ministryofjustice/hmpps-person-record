package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.util.UUID

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?

  fun findByCId(cId: String): PersonEntity?

  fun findByCrn(crn: String): PersonEntity?

  fun findByPrisonNumber(prisonNumber: String): PersonEntity? = findByPrisonNumberAndSourceSystem(prisonNumber, NOMIS)

  fun findByPrisonNumberAndSourceSystem(prisonNumber: String, sourceSystem: SourceSystemType): PersonEntity?

  fun findByMergedTo(mergedTo: Long): List<PersonEntity?>

  fun findByMatchId(matchId: UUID): PersonEntity?

  fun countBySourceSystemAndMergedToIsNull(sourceSystem: SourceSystemType): Long

  fun findAllByEthnicityIsNotNullOrderById(pageable: Pageable): Page<PersonEntity>
}
