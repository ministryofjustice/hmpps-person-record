package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.CRO
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.NI
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.PNC
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

fun findCandidates(person: Person): Specification<PersonEntity> {
  val postcodes = person.addresses.mapNotNull { it.postcode }

  val soundexFirstLastName = Specification.where(
    PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
      .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
  )
  val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
  val levenshteinPostcode = Specification.where(PersonSpecification.levenshteinPostcodes(postcodes))
  return Specification.where(
    exactMatch(person.otherIdentifiers?.pncIdentifier?.toString(), PNC)
      .or(exactMatch(person.driverLicenseNumber, DRIVER_LICENSE_NUMBER))
      .or(exactMatch(person.nationalInsuranceNumber, NI))
      .or(exactMatch(person.otherIdentifiers?.croIdentifier?.toString(), CRO))
      .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode))),
  )
}

fun findCandidatesBySourceSystem(person: Person): Specification<PersonEntity> {
  return findCandidates(person).and(exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM))
}
