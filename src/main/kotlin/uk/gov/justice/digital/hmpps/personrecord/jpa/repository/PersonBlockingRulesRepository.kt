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

    val results = query.resultList as List<PersonEntity>

    return PageImpl(results, pageable, results.size.toLong())
  }
}
