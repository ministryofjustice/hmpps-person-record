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
    searchQueryBuilder.append("SELECT DISTINCT(p.*) from person p ")
    searchQueryBuilder.append("INNER JOIN defendant d on d.fk_person_id = p.id ")
    searchQueryBuilder.append("WHERE d.surname ILIKE :surname ")
    personSearchRequest.forenameOne?.let { searchQueryBuilder.append("AND d.forename_one ILIKE :forenameOne ") }
    personSearchRequest.forenameTwo?.let { searchQueryBuilder.append("AND d.forename_two ILIKE :forenameTwo ") }
    personSearchRequest.forenameThree?.let { searchQueryBuilder.append("AND d.forename_three ILIKE :forenameThree ") }
    personSearchRequest.dateOfBirth?.let { searchQueryBuilder.append("AND d.date_of_birth = :dateOfBirth ") }
    personSearchRequest.pncNumber?.let { searchQueryBuilder.append("AND d.pnc_number ILIKE :pncNumber ") }

    val personQuery: Query = entityManager.createNativeQuery(searchQueryBuilder.toString(), PersonEntity::class.java)
    personSearchRequest.surname.let { personQuery.setParameter("surname", it) }
    personSearchRequest.forenameOne?.let { personQuery.setParameter("forenameOne", it) }
    personSearchRequest.forenameTwo?.let { personQuery.setParameter("forenameTwo", it) }
    personSearchRequest.forenameThree?.let { personQuery.setParameter("forenameThree", it) }
    personSearchRequest.dateOfBirth?.let { personQuery.setParameter("dateOfBirth", it) }
    personSearchRequest.pncNumber?.let { personQuery.setParameter("pncNumber", it) }

    return personQuery.resultList as List<PersonEntity>
  }
}
