package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria

@Component
class PersonBlockingRulesRepository {
  @PersistenceContext
  private val entityManager: EntityManager? = null

  fun findMatchCandidates(searchCriteria: PersonSearchCriteria, personQuery: String, pageable: Pageable, totalResults: Long?): Page<PersonEntity> {
    val query = entityManager!!.createNativeQuery(personQuery, PersonEntity::class.java)

    query.setParameter("firstName", searchCriteria.firstName)
    query.setParameter("lastName", searchCriteria.lastName)

    // apply pagination
    query.firstResult = pageable.offset.toInt()
    query.maxResults = pageable.pageSize

    val results = query.resultList as List<PersonEntity>

    return PageImpl(results, pageable, totalResults ?: 0L)
  }

  fun countMatchCandidates(personQuery: String, person: PersonSearchCriteria): Long {
    val queryString = "SELECT COUNT (*) FROM ($personQuery) AS total"

    val query = entityManager!!.createNativeQuery(queryString)

    query.setParameter("firstName", person.firstName)
    query.setParameter("lastName", person.lastName)

    return (query.singleResult as Number).toLong()
  }
}
