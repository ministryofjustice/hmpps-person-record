package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity

@Repository
interface ReferenceRepository : JpaSpecificationExecutor<ReferenceEntity>, JpaRepository<ReferenceEntity, Long> {

  @Query(
    """
    SELECT
        ref.identifier_value as term, 
        CAST(COUNT(*) AS FLOAT8) / (
            SELECT 
                COUNT(ref_inner.identifier_value) AS total 
            FROM 
                personrecordservice.reference ref_inner
            WHERE 
                ref_inner.identifier_type = :identifierType
                AND ref_inner.identifier_value IS NOT null
        ) AS frequency
    FROM
        personrecordservice.reference ref
    WHERE
        ref.identifier_type = :identifierType
        AND ref.identifier_value IS NOT null
    GROUP BY
        ref.identifier_value
    """,
    countQuery = """
      SELECT 
        COUNT(distinct ref.identifier_value) 
      FROM 
          personrecordservice.reference ref
      WHERE 
          ref.identifier_type = 'PNC' AND ref.identifier_value IS NOT null;
    """,
    nativeQuery = true,
  )
  fun getIdentifierTermFrequency(@Param("identifierType") identifierType: String, pageable: Pageable): Page<TermFrequency>
}
