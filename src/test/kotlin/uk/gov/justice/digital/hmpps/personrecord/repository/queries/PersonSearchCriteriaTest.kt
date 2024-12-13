package uk.gov.justice.digital.hmpps.personrecord.repository.queries

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM

class PersonSearchCriteriaTest {

  @Test
  fun `should deduplicate postcodes from person object before building query`() {
    val person = Person(
      addresses = listOf(
        Address(postcode = "AB1 1AB"),
        Address(postcode = "AB1 1AB"),
        Address(postcode = "BC2 2BC"),
        Address(postcode = "BC2 2BC"),
        Address(postcode = "CD3 DC3"),
      ),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(person)
    assertThat(searchCriteria.postcodes.size).isEqualTo(3)
    assertThat(searchCriteria.postcodes).isEqualTo(setOf("AB1 1AB", "BC2 2BC", "CD3 DC3"))
  }

  @Test
  fun `should deduplicate postcodes from person entity before building query`() {
    val personEntity = PersonEntity(
      addresses = mutableListOf(
        AddressEntity(postcode = "AB1 1AB"),
        AddressEntity(postcode = "AB1 1AB"),
        AddressEntity(postcode = "BC2 2BC"),
        AddressEntity(postcode = "BC2 2BC"),
        AddressEntity(postcode = "CD3 DC3"),
      ),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(personEntity)
    assertThat(searchCriteria.postcodes.size).isEqualTo(3)
    assertThat(searchCriteria.postcodes).isEqualTo(setOf("AB1 1AB", "BC2 2BC", "CD3 DC3"))
  }

  @Test
  fun `should deduplicate identifiers from person object before building query`() {
    val person = Person(
      references = listOf(
        Reference(identifierType = CRO, identifierValue = "12345"),
        Reference(identifierType = CRO, identifierValue = "12345"),
        Reference(identifierType = PNC, identifierValue = "12345"),
        Reference(identifierType = PNC, identifierValue = "12345"),
        Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345"),
      ),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(person)
    assertThat(searchCriteria.identifiers.size).isEqualTo(3)
    assertThat(searchCriteria.identifiers).isEqualTo(
      setOf(
        Reference(identifierType = CRO, identifierValue = "12345"),
        Reference(identifierType = PNC, identifierValue = "12345"),
        Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345"),
      ),
    )
  }

  @Test
  fun `should deduplicate identifiers from person entity before building query`() {
    val person = PersonEntity(
      references = mutableListOf(
        ReferenceEntity(identifierType = CRO, identifierValue = "12345"),
        ReferenceEntity(identifierType = CRO, identifierValue = "12345"),
        ReferenceEntity(identifierType = PNC, identifierValue = "12345"),
        ReferenceEntity(identifierType = PNC, identifierValue = "12345"),
        ReferenceEntity(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345"),
      ),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(person)
    assertThat(searchCriteria.identifiers.size).isEqualTo(3)
    assertThat(searchCriteria.identifiers).isEqualTo(
      setOf(
        Reference(identifierType = CRO, identifierValue = "12345"),
        Reference(identifierType = PNC, identifierValue = "12345"),
        Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345"),
      ),
    )
  }
}
