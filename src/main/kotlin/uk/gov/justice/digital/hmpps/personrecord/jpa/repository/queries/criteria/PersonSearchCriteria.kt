package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate

data class PersonSearchCriteria(
  val id: Long? = null,
  val firstName: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val identifiers: List<Reference> = listOf(),
  val postcodes: List<String> = listOf(),
  val sourceSystemType: SourceSystemType,
) {
  companion object {
    private val SEARCH_IDENTIFIERS = listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER)

    fun from(person: Person): PersonSearchCriteria = PersonSearchCriteria(
      firstName = person.firstName,
      lastName = person.lastName,
      dateOfBirth = person.dateOfBirth,
      identifiers = person.getIdentifiersForMatching(SEARCH_IDENTIFIERS),
      postcodes = person.addresses.mapNotNull { it.postcode },
      sourceSystemType = person.sourceSystemType,
    )

    fun from(personEntity: PersonEntity): PersonSearchCriteria = PersonSearchCriteria(
      id = personEntity.id,
      firstName = personEntity.firstName,
      lastName = personEntity.lastName,
      dateOfBirth = personEntity.dateOfBirth,
      identifiers = personEntity.getIdentifiersForMatching(SEARCH_IDENTIFIERS).map {
        Reference(identifierType = it.identifierType, identifierValue = it.identifierValue)
      },
      postcodes = personEntity.addresses.mapNotNull { it.postcode },
      sourceSystemType = personEntity.sourceSystem,
    )
  }
}
