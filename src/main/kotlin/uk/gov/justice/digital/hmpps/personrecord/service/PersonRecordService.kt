package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerEntity
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

  fun createNewPersonAndDefendant(person: Person): PersonEntity {
    log.debug("Entered createNewPersonAndDefendant with pnc ${person.otherIdentifiers?.pncIdentifier}")

    val newPersonEntity = PersonEntity.new()
    val newDefendantEntity = createDefendant(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)

    return personRepository.saveAndFlush(newPersonEntity)
  }

  private fun createDefendant(person: Person): DefendantEntity {
    val newDefendantEntity = DefendantEntity.from(person)

    val defendantAliases = DefendantAliasEntity.fromList(person.personAliases)
    defendantAliases.forEach { defendantAliasEntity -> defendantAliasEntity.defendant = newDefendantEntity }
    newDefendantEntity.aliases.addAll(defendantAliases)

    return newDefendantEntity
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
