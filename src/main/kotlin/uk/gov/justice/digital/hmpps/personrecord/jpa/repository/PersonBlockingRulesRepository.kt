package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class PersonBlockingRulesRepository {
  @PersistenceContext
  private val entityManager: EntityManager? = null

  fun findMatchCandidates(person: Person, personQuery: String, pageable: Pageable): Page<PersonEntity> {
    val query = entityManager!!.createNativeQuery(personQuery, PersonEntity::class.java)
    query.setParameter("firstName", person.firstName)
    query.setParameter("lastName", person.lastName)

    // apply pagination
    query.setFirstResult((pageable.offset.toInt()))
    query.maxResults = pageable.pageSize

    val results = query.resultList as List<PersonEntity>

    val countQuery = countMatchCandidates(personQuery, person)
    return PageImpl(results, pageable, countQuery)
  }

  fun countMatchCandidates(personQuery: String, person: Person): Long {
    val queryString = "SELECT COUNT (*) FROM ($personQuery) AS total"

    println("executing count query " + queryString)

    val query = entityManager!!.createNativeQuery(queryString)

    query.setParameter("firstName", person.firstName)
    query.setParameter("lastName", person.lastName)

    return (query.singleResult as Number).toLong()
  }
}
