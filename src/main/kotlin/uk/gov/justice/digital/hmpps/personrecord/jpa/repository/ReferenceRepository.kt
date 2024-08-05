package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity

@Repository
interface ReferenceRepository : JpaSpecificationExecutor<ReferenceEntity>, JpaRepository<ReferenceEntity, Long> {
  @Query(
    """
    SELECT new uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency(
        r.identifierValue, 
        CAST(COUNT(*) AS BigDecimal) / (
          SELECT 
            COUNT(r.identifierValue) AS total 
          FROM 
            ReferenceEntity r
          WHERE 
            r.identifierType = 'PNC' and r.identifierValue IS NOT null
        )
      )
    FROM
        ReferenceEntity r
    WHERE
        r.identifierType = 'PNC' and r.identifierValue IS NOT null
    GROUP BY
        r.identifierValue
  """,
  )
  fun getTermFrequencyForPnc(): List<TermFrequency>
}
