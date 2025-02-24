package uk.gov.justice.digital.hmpps.personrecord.repository.queries

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedIdentifierStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedStringStatement
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import java.util.*

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
    assertThat(searchCriteria.preparedPostcodes.size).isEqualTo(3)
    assertThat(searchCriteria.preparedPostcodes).isEqualTo(
      listOf(
        PreparedStringStatement("postcode0", "AB1"),
        PreparedStringStatement("postcode1", "BC2"),
        PreparedStringStatement("postcode2", "CD3"),
      ),
    )
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
      matchId = UUID.randomUUID(),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(personEntity)
    assertThat(searchCriteria.preparedPostcodes.size).isEqualTo(3)
    assertThat(searchCriteria.preparedPostcodes).isEqualTo(
      listOf(
        PreparedStringStatement("postcode0", "AB1"),
        PreparedStringStatement("postcode1", "BC2"),
        PreparedStringStatement("postcode2", "CD3"),
      ),
    )
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
    assertThat(searchCriteria.preparedIdentifiers.size).isEqualTo(3)
    assertThat(searchCriteria.preparedIdentifiers).isEqualTo(
      listOf(
        PreparedIdentifierStatement("identifier0", reference = Reference(identifierType = CRO, identifierValue = "12345")),
        PreparedIdentifierStatement("identifier1", reference = Reference(identifierType = PNC, identifierValue = "12345")),
        PreparedIdentifierStatement("identifier2", reference = Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345")),
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
      matchId = UUID.randomUUID(),
      sourceSystem = COMMON_PLATFORM,
    )
    val searchCriteria = PersonSearchCriteria.from(person)
    assertThat(searchCriteria.preparedIdentifiers.size).isEqualTo(3)
    assertThat(searchCriteria.preparedIdentifiers).isEqualTo(
      listOf(
        PreparedIdentifierStatement("identifier0", reference = Reference(identifierType = CRO, identifierValue = "12345")),
        PreparedIdentifierStatement("identifier1", reference = Reference(identifierType = PNC, identifierValue = "12345")),
        PreparedIdentifierStatement("identifier2", reference = Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = "12345")),
      ),
    )
  }
}
