package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

class PersonServiceIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var personService: PersonService

  @BeforeEach
  override fun beforeEach() {
    // need to delete as obscure fields to search on???
    personRepository.deleteAll()
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2003/0011985X")),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.size).isEqualTo(1)
    assertThat(personEntities[0].pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val personToFind = Person(
      driverLicenseNumber = "01234567890",
      sourceSystemType = SourceSystemType.HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = "0987654321",
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.size).isEqualTo(1)
    assertThat(personEntities[0].driverLicenseNumber).isEqualTo("01234567890")
  }

  @Test
  fun `should find candidate records on exact matches on national insurance number`() {
    val personToFind = Person(
      nationalInsuranceNumber = "PG1234567C",
      sourceSystemType = SourceSystemType.HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        nationalInsuranceNumber = "RF9876543C",
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.size).isEqualTo(1)
    assertThat(personEntities[0].nationalInsuranceNumber).isEqualTo("PG1234567C")
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from("86621/65B")),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from("51072/62R")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.size).isEqualTo(1)
    assertThat(personEntities[0].cro).isEqualTo(CROIdentifier.from("86621/65B"))
  }

  private fun createPerson(person: Person): PersonEntity {
    return personRepository.saveAndFlush(PersonEntity.from(person))
  }
}
