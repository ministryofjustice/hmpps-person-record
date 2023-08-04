package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.util.*

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPersonById(id: UUID): Person {
    log.debug("Entered getPersonById($id)")
    return Person.from(
      personRepository.findByPersonId(id)
        ?: throw EntityNotFoundException("Person record not found for id: $id"),
    )
  }

  fun createPersonRecord(person: Person): Person {
    log.debug("Entered createPersonRecord()")

    val personEntity = personRepository.save(PersonEntity.from(person))
    return Person.from(personEntity)
  }

  fun searchPersonRecords(searchRequest: PersonSearchRequest): List<Person> {
    log.debug("Entered searchPersonRecords()")

    searchRequest.crn?.let {
      return listOf(Person.from(
        personRepository.findByDeliusOffendersCrn(it) ?: throw EntityNotFoundException("Person record not found for crn: $it")
      ))
    }

    // ensure minimum parameters are present in the search request
    if (StringUtils.isEmpty(searchRequest.surname)) {
      throw ValidationException("Surname not provided in search request")
    }

    return personRepository.searchByRequestParameters(searchRequest)
      .map { Person.from(it) }
  }
}
