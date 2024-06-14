package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate

class PersonServiceIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var personService: PersonService

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    personRepository.deleteAll()
  }

  @Test
  fun `should find candidate records only in searching source system`() {
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2003/0011985X")),
      sourceSystemType = SourceSystemType.LIBRA,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = SourceSystemType.NOMIS,
      ),
    )
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = SourceSystemType.DELIUS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
    assertThat(personEntities.get().findFirst().get().sourceSystem).isEqualTo(SourceSystemType.LIBRA)
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

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
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

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().driverLicenseNumber).isEqualTo("01234567890")
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

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().nationalInsuranceNumber).isEqualTo("PG1234567C")
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

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().cro).isEqualTo(CROIdentifier.from("86621/65B"))
  }

  @Test
  fun `should find candidate records on soundex matches on first name`() {
    createPerson(
      Person(
        firstName = "Steven",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Micheal",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().firstName).isEqualTo("Steven")
  }

  @Test
  fun `should find candidate records on soundex matches on last name`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smythe",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().lastName).isEqualTo("Smith")
  }

  @Test
  fun `should find candidate records on Levenshtein matches on dob`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1986, 4, 2),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on Levenshtein matches on postcode`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        addresses = listOf(Address(postcode = "ZB5 78O")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      addresses = listOf(Address(postcode = "LS2 1AC"), Address(postcode = "LD2 3BC")),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(personEntities.get().findFirst().get().addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  @Test
  fun `should not find candidate records on matching postcode but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        lastName = "Micheal",
        addresses = listOf(Address(postcode = "ZB5 78O")),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(0)
  }

  @Test
  fun `should not find candidate records on matching dob but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )
    createPerson(
      Person(
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1988, 4, 5),
        sourceSystemType = SourceSystemType.HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = SourceSystemType.HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(0)
  }

  private fun createPerson(person: Person): PersonEntity {
    return personRepository.saveAndFlush(PersonEntity.from(person))
  }
}
