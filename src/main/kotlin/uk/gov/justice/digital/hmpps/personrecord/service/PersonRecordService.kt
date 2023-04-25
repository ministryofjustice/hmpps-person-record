package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.util.UUID

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPersonById(id: UUID): PersonDetails {
    log.debug("Entered getPersonById($id)")
    return PersonDetails.from(
      personRepository.findByPersonId(id)
        ?: throw EntityNotFoundException("Person record not found for id: $id"),
    )
  }

  fun createPersonRecord(person: PersonDetails): PersonDetails {
    log.debug("Entered createPersonRecord()")

    val personEntity = personRepository.save(PersonEntity.from(person))
    return PersonDetails.from(personEntity)
  }

  fun searchPersonRecords(searchRequest: PersonSearchRequest): List<PersonDetails> {
    log.debug("Entered searchPersonRecords()")

    return personRepository.searchByRequestParameters(searchRequest)
      .map { PersonDetails.from(it) }
  }
}
