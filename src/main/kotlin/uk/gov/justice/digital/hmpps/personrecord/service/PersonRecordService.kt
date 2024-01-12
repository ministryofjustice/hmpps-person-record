package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.util.*

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
  val offenderRepository: OffenderRepository,
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
    log.debug("Entered createPersonRecord with")

    // create an offender
    person.otherIdentifiers?.crn?.let {
      if (!doesOffenderExistInPersonRecord(person.otherIdentifiers.crn)) {
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
      log.error("Multiple person records exist for search criteria")
      throw IllegalArgumentException("Multiple person records exist for search criteria")
    }
  }

  private fun searchDeliusOffender(person: Person): OffenderDetail? {
    return offenderSearchClient.getOffenderDetail(SearchDto.from(person))?.getOrNull(0)
  }

  private fun doesOffenderExistInPersonRecord(crn: String): Boolean {
    return offenderRepository.existsByCrn(crn)
  }

  private fun createOffenderFromOffenderDetail(offenderDetail: OffenderDetail): PersonEntity {
    val newPersonEntity = PersonEntity.new()
    // create an offender
    val newOffenderEntity = OffenderEntity.from(offenderDetail)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.offenders.add(newOffenderEntity)
    return personRepository.saveAndFlush(newPersonEntity)
  }

  fun createDefendantFromPerson(person: Person): PersonEntity {
    log.debug("Entered createDefendantFromPerson")

    val newPersonEntity = PersonEntity.new()

    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)
    return personRepository.saveAndFlush(newPersonEntity)
  }

  private fun addDefendantToPerson(personEntity: PersonEntity, person: Person): PersonEntity {
    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = personEntity
    personEntity.defendants.add(newDefendantEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  fun addOffenderToPerson(personEntity: PersonEntity, person: Person): PersonEntity {
    val offenderEntity = OffenderEntity.from(person)
    offenderEntity.person = personEntity
    personEntity.offenders.add(offenderEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  fun addPrisonerToPerson(personEntity: PersonEntity, prisoner: Prisoner): PersonEntity {
    val prisonerEntity = PrisonerEntity.from(prisoner)
    prisonerEntity.person = personEntity
    personEntity.prisoners.add(prisonerEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun createOffenderFromPerson(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.new()
    val newOffenderEntity = OffenderEntity.from(person)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.offenders.add(newOffenderEntity)
    return personRepository.saveAndFlush(newPersonEntity)
  }

  fun searchPersonRecords(searchRequest: PersonSearchRequest): List<Person> {
    log.debug("Entered searchPersonRecords()")

    searchRequest.crn?.let {
      return listOf(
        Person.from(
          personRepository.findByOffendersCrn(it) ?: throw EntityNotFoundException("Person record not found for crn: $it"),
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
