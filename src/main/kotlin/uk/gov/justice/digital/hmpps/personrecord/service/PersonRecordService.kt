package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DeliusOffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.HmctsDefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DeliusOffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.lang.IllegalArgumentException
import java.util.*

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
  val deliusOffenderRepository: DeliusOffenderRepository,
  val offenderSearchClient: ProbationOffenderSearchClient,
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
    log.debug("Entered createPersonRecord with $person")

    // create an offender
    person.otherIdentifiers?.crn?.let {
      if (!isOffenderExistInPersonRecord(person.otherIdentifiers.crn)) {
        val offenderDetail = searchDeliusOffender(person)
        if (offenderDetail != null) {
          return Person.from(createOffenderFromOffenderDetail(offenderDetail))
        } else {
          return Person.from(createOffenderFromPerson(person))
        }
      } else {
        log.error("Person with ${person.otherIdentifiers.crn} already exist")
        throw DataIntegrityViolationException("Person already exist")
      }
    }

    // Create a new defendant or updating an existing defendant list
    val searchRequest = PersonSearchRequest.from(person)
    val existingPersons = personRepository.searchByRequestParameters(searchRequest)
    return if (existingPersons.isEmpty()) {
      Person.from(createDefendantFromPerson(person))
    } else if (existingPersons.size == 1) { // exact match
      Person.from(addDefendantToPerson(existingPersons[0], person))
    } else {
      log.error("Multiple person records exist for search criteria $person")
      throw IllegalArgumentException("Multiple person records exist for search criteria $person")
    }
  }

  private fun searchDeliusOffender(person: Person): OffenderDetail? {
    val offenderDetails = offenderSearchClient.getOffenderDetail(SearchDto.from(person))
    if (!offenderDetails.isNullOrEmpty()) return offenderSearchClient.getOffenderDetail(SearchDto.from(person))?.get(0)
    return null
  }
  private fun isOffenderExistInPersonRecord(crn: String): Boolean {
    return deliusOffenderRepository.existsByCrn(crn)
  }
  private fun createOffenderFromOffenderDetail(offenderDetail: OffenderDetail): PersonEntity {
    val newPersonEntity = PersonEntity.new()
    // create an offender
    val newOffenderEntity = DeliusOffenderEntity.from(offenderDetail)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.deliusOffenders.add(newOffenderEntity)
    return personRepository.save(newPersonEntity)
  }

  private fun createDefendantFromPerson(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.new()

    val newDefendantEntity = HmctsDefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.hmctsDefendants.add(newDefendantEntity)
    return personRepository.save(newPersonEntity)
  }

  private fun addDefendantToPerson(personEntity: PersonEntity, person: Person): PersonEntity {
    val newDefendantEntity = HmctsDefendantEntity.from(person)
    newDefendantEntity.person = personEntity
    personEntity.hmctsDefendants.add(newDefendantEntity)
    return personRepository.save(personEntity)
  }

  private fun createOffenderFromPerson(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.new()
    val newOffenderEntity = DeliusOffenderEntity.from(person)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.deliusOffenders.add(newOffenderEntity)
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
}
