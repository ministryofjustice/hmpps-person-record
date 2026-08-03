package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDateTime
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

  fun countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(sourceSystem: SourceSystemType): Long

  fun findByLastModifiedAfter(
    lastModifiedAfter: LocalDateTime,
  ): MutableList<PersonEntity>

  @Query(
    value = """
            SELECT *
            FROM personrecordservice.person p
            WHERE p.religion NOT IN (
              'ADV','AGNO','APO','ATHE','BAHA','BAPT','BLAC','BUDD','CALV',
              'CCOG','CE','CHJCLDS','CHRODX','CHRST','CHSC','CINW','COFE',
              'COFI','COFN','COFS','CONG','COPT','CSW','DRU','EODX','EORTH',
              'EPIS','ETHO','EVAN','GOSP','GROX','HARE','HIND','HNDHAR','HUM',
              'JAIN','JEHV','JEW','LUTH','METH','MORM','MOS','MUSOTH','NIL',
              'NONC','NONP','OORTH','ORTH','OTH','PAG','PAGDRU','PENT','PRES',
              'PROT','QUAK','RAST','RC','RUSS','SALV','SATN','SCIE','SDAY',
              'SHIA','SHIN','SHNTAO','SIKH','SPIR','SUNI','TAO','TPRNTS',
              'UNIF','UNIT','UNKN','UR','WELS','ZORO'
            )
        """,
    nativeQuery = true,
  )
  fun findAllPersonsWithOldReligionCodes(): List<PersonEntity>
}
