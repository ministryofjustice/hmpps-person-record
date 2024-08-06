package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency.PncFrequencyEntity

@Repository
interface PncFrequencyRepository : CrudRepository<PncFrequencyEntity, Long>, ListCrudRepository<PncFrequencyEntity, Long> {

  @Transactional
  @Modifying
  @Query(
    """
    INSERT INTO personmatchscore.pnc_frequency (pnc, frequency)
    SELECT
        ref.identifier_value as term, 
        CAST(COUNT(*) AS FLOAT8) / (
            SELECT 
                COUNT(ref_inner.identifier_value) AS total 
            FROM 
                personrecordservice.reference ref_inner
            WHERE 
                ref_inner.identifier_type = 'PNC'
                AND ref_inner.identifier_value IS NOT null
        ) AS frequency
    FROM
        personrecordservice.reference ref
    WHERE
        ref.identifier_type = 'PNC'
        AND ref.identifier_value IS NOT null
    GROUP BY
        ref.identifier_value
    """,
    nativeQuery = true,
  )
  fun generatePncTermFrequency(): Int
}
