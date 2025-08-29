package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity

data class PersonOverridePair(
  val firstPersonId: Long,
  val secondPersonId: Long,
)

@Repository
interface MarkerRepository : JpaRepository<OverrideMarkerEntity, Long> {

  @Query(
    """
      select distinct 
        least(om1.fkPersonId, om2.fkPersonId) as first_person_id,
        greatest(om1.fkPersonId, om2.fkPersonId) as second_person_id
      from OverrideMarkerEntity om1
      join OverrideMarkerEntity om2
        on om1.fkPersonId = om2.markerValue
        and om2.fkPersonId = om1.markerValue
      where om1.markerType = 'EXCLUDE'
    """,
  )
  fun findDistinctPairsOfOverrideMarkers(): List<PersonOverridePair?>?
}
