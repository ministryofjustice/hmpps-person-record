package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.termfrequency

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TermCleaningRepository(
  private val entityManager: EntityManager,
) {

  @Transactional
  fun refreshPersonAggregateData() = entityManager.createNativeQuery(REFRESH_PERSON_AGGREGATE_SQL).executeUpdate()

  companion object {
    private const val REFRESH_PERSON_AGGREGATE_SQL = """
      REFRESH MATERIALIZED VIEW CONCURRENTLY personrecordservice.person_aggregate_data;
    """
  }
}
