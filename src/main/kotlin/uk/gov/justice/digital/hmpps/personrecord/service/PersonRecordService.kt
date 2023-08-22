package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DeliusOffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.HmctsDefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DeliusOffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.HmctsDefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.lang.IllegalArgumentException
import java.util.*

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
  val deliusOffenderRepository: DeliusOffenderRepository,
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

    // create an offender as well as a defendant record and link to a person
    person.otherIdentifiers?.crn?.let {
      return createOffender(person)
    }

    // Create a new defendant or updating an existing defendant list
    val searchRequest = PersonSearchRequest.from(person)
    val existingDefendants = personRepository.searchByRequestParameters(searchRequest)
    if (existingDefendants.isEmpty() || existingDefendants.size == 1) {

      return Person.from(createDefendant(person))
    }

    return person;
  }

  private fun createOffender(person: Person): Person {
    val offenderFromPersonRecord = person.otherIdentifiers?.crn?.let { searchOffenderFromPersonRecord(it) };
    if (offenderFromPersonRecord == null) {
      //TODO nDelius offender check
      return Person.from(createOffenderAndDefendant(person))
    } else {
      throw IllegalArgumentException("Offender already exist")
    }
  }

  private fun createOffenderAndDefendant(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.from(person);
    //create an offender
    val newOffenderEntity = DeliusOffenderEntity.from(person)
    newOffenderEntity?.person = newPersonEntity
    if (newOffenderEntity != null) {
      newPersonEntity.deliusOffenders.add(newOffenderEntity)
    }
    //create a defendant
    val newDefendantEntity = HmctsDefendantEntity.from(person)
    newDefendantEntity?.person = newPersonEntity
    if (newDefendantEntity != null) {
      newPersonEntity.hmctsDefendants.add(newDefendantEntity)
    }
    return personRepository.save(newPersonEntity)
  }

  private fun createDefendant(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.from(person);
    val newDefendantEntity = HmctsDefendantEntity.from(person)
    newDefendantEntity?.person = newPersonEntity
    if (newDefendantEntity != null) {
      newPersonEntity.hmctsDefendants.add(newDefendantEntity)
    }
    return personRepository.save(newPersonEntity)
  }

  fun searchPersonRecords(searchRequest: PersonSearchRequest): List<Person> {
    log.debug("Entered searchPersonRecords()")

    searchRequest.crn?.let {
      return listOf(
        Person.from(
          personRepository.findByDeliusOffendersCrn(it) ?: throw EntityNotFoundException("Person record not found for crn: $it"),
        ),
      )
    }

    // ensure minimum parameters are present in the search request
    if (StringUtils.isEmpty(searchRequest.surname)) {
      throw ValidationException("Surname not provided in search request")
    }

    return personRepository.searchByRequestParameters(searchRequest)
      .map { Person.from(it) }
  }
  private fun searchOffenderFromPersonRecord(crn: String): DeliusOffenderEntity? {
    return deliusOffenderRepository.findByCrn(crn)
  }
}
