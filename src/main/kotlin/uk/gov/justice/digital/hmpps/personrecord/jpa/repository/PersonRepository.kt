package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS

@Repository
interface PersonRepository : JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?

  fun findByDefendantIdAndSourceSystem(defendantId: String, sourceSystem: SourceSystemType? = COMMON_PLATFORM): PersonEntity?

  fun findByCrn(crn: String): PersonEntity?

  fun findByCrnAndSourceSystem(crn: String, sourceSystem: SourceSystemType? = DELIUS): PersonEntity?

  fun findByPrisonNumberAndSourceSystem(prisonNumber: String, sourceSystem: SourceSystemType? = NOMIS): PersonEntity?
}
