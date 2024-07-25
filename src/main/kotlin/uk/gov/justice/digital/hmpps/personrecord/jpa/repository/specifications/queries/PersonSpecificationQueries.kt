package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatchReferences
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

private fun findCandidates(person: Person): Specification<PersonEntity> {
  val postcodes = person.addresses.mapNotNull { it.postcode }.toSet()
  val references = person.references.filter {
    listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER).contains(it.identifierType)
  }

  val soundexFirstLastName = Specification.where(
    PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
      .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
  )
  val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
  val levenshteinPostcode = Specification.where(PersonSpecification.levenshteinPostcodes(postcodes))

  return Specification.where(
    exactMatchReferences(references)
      .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode))),
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
