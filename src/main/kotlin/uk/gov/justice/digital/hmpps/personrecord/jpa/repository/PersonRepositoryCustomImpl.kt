package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest

@Repository
class PersonRepositoryCustomImpl : PersonRepositoryCustom {

  @PersistenceContext
  lateinit var entityManager: EntityManager

  override fun searchByRequestParameters(personSearchRequest: PersonSearchRequest): List<PersonEntity> {
    val searchQueryBuilder = StringBuilder()
    searchQueryBuilder.append("SELECT p.* from person p ")
    searchQueryBuilder.append("WHERE p.family_name ILIKE :surname ")
    personSearchRequest.forename?.let { searchQueryBuilder.append("AND p.given_name ILIKE :forename ") }
    personSearchRequest.middleNames?.let { searchQueryBuilder.append("AND p.middle_names ILIKE :middleNames ") }
    personSearchRequest.dateOfBirth?.let { searchQueryBuilder.append("AND p.date_of_birth = :dateOfBirth ") }
    personSearchRequest.pncNumber?.let { searchQueryBuilder.append("AND p.pnc_number ILIKE :pncNumber ") }
    personSearchRequest.crn?.let { searchQueryBuilder.append("AND p.crn ILIKE :crn ") }

    val personQuery: Query = entityManager.createNativeQuery(searchQueryBuilder.toString(), PersonEntity::class.java)
    personSearchRequest.surname?.let { personQuery.setParameter("surname", it) }
    personSearchRequest.forename?.let { personQuery.setParameter("forename", it) }
    personSearchRequest.middleNames?.let { personQuery.setParameter("middleNames", it) }
    personSearchRequest.dateOfBirth?.let { personQuery.setParameter("dateOfBirth", it) }
    personSearchRequest.pncNumber?.let { personQuery.setParameter("pncNumber", it) }
    personSearchRequest.crn?.let { personQuery.setParameter("crn", it) }

    return personQuery.resultList as List<PersonEntity>
  }
}
