package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatchReferences
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

private fun findCandidates(person: Person): Specification<PersonEntity> {
  val postcodes = person.addresses.mapNotNull { it.postcode }.toSet()
  val references = person.references.filter { PersonSpecification.SEARCH_IDENTIFIERS.contains(it.identifierType) && !it.identifierValue.isNullOrEmpty() }

  val soundexFirstLastName = Specification.where(
    PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
      .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
  )
  val hasTwoDateParts = Specification.where(PersonSpecification.matchDateParts(person.dateOfBirth, PersonSpecification.DOB))
  val matchesPostcodePrefix = Specification.where(PersonSpecification.exactMatchPostcodePrefix(postcodes))

  return Specification.where(
    exactMatchReferences(references)
      .or(soundexFirstLastName.and(hasTwoDateParts.or(matchesPostcodePrefix)))
      .and(PersonSpecification.isNotMerged()),
  )
}

fun findCandidatesWithUuid(person: Person): PersonQuery = PersonQuery(
  queryName = PersonQueryType.FIND_CANDIDATES_WITH_UUID,
  query = findCandidates(person).and(PersonSpecification.hasPersonKey()),
)

fun findCandidatesBySourceSystem(person: Person): PersonQuery = PersonQuery(
  queryName = PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM,
  query = findCandidates(person).and(exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM)),
)
