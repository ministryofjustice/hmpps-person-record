package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria

@Component
class PersonBlockingRulesRepository {
  @PersistenceContext
  private val entityManager: EntityManager? = null

  fun findMatchCandidates(searchCriteria: PersonSearchCriteria, personQuery: String): List<PersonEntity> {
    val query = entityManager!!.createNativeQuery(personQuery, PersonEntity::class.java)
    query.setParameter("firstName", searchCriteria.firstName)
    query.setParameter("lastName", searchCriteria.lastName)
    return query.resultList as List<PersonEntity>
  }
}
