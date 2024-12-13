package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
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

  fun findMatchCandidates(searchCriteria: PersonSearchCriteria, personQuery: String, pageable: Pageable, totalResults: Long = 0L): Page<PersonEntity> {
    val query = entityManager!!.createNativeQuery(personQuery, PersonEntity::class.java)

    query.buildParameters(searchCriteria)

    // apply pagination
    query.firstResult = pageable.offset.toInt()
    query.maxResults = pageable.pageSize

    val results = query.resultList as List<PersonEntity>

    return PageImpl(results, pageable, totalResults)
  }

  fun countMatchCandidates(personQuery: String, searchCriteria: PersonSearchCriteria): Long {
    val queryString = "SELECT COUNT (*) FROM ($personQuery) AS total"

    val query = entityManager!!.createNativeQuery(queryString)

    query.buildParameters(searchCriteria)

    return (query.singleResult as Number).toLong()
  }

  companion object {

    private fun Query.buildParameters(searchCriteria: PersonSearchCriteria) {
      searchCriteria.preparedId?.let { this.setParameter(it.parameterName, it.value) }
      this.setParameter(searchCriteria.preparedFirstName.parameterName, searchCriteria.preparedFirstName.value)
      this.setParameter(searchCriteria.preparedLastName.parameterName, searchCriteria.preparedLastName.value)

      this.setParameter(searchCriteria.preparedDateOfBirth.day.parameterName, searchCriteria.preparedDateOfBirth.day.value)
      this.setParameter(searchCriteria.preparedDateOfBirth.month.parameterName, searchCriteria.preparedDateOfBirth.month.value)
      this.setParameter(searchCriteria.preparedDateOfBirth.year.parameterName, searchCriteria.preparedDateOfBirth.year.value)

      searchCriteria.preparedPostcodes.forEach {
        this.setParameter(it.parameterName, it.value)
      }
      searchCriteria.preparedIdentifiers.forEach {
        this.setParameter(it.parameterName, it.reference.identifierValue)
      }
    }
  }
}
