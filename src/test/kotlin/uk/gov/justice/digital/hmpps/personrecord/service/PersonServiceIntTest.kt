package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.HMCTS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate

class PersonServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personService: PersonService

  @Autowired
  private lateinit var personRepository: PersonRepository

  @BeforeEach
  override fun beforeEach() {
    personRepository.deleteAll()
  }

  @Test
  fun `should find candidate records only in searching source system`() {
    val personToFind = Person(
      firstName = "Stephen",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = NOMIS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = DELIUS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2003/0011985X")),
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
  }

  @Test
  fun `should not find candidates which only match on empty PNC`() {
    val personToFind = Person(
      firstName = "Miroslav",
      lastName = "Klose",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("")),
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        firstName = "Horst",
        lastName = "Hrubesch",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("")),
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).firstName).isEqualTo("Miroslav")
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val personToFind = Person(
      driverLicenseNumber = "01234567890",
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = "0987654321",
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).driverLicenseNumber).isEqualTo("01234567890")
  }

  @Test
  fun `should find candidate records on exact matches on national insurance number`() {
    val personToFind = Person(
      nationalInsuranceNumber = "PG1234567C",
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        nationalInsuranceNumber = "RF9876543C",
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).nationalInsuranceNumber).isEqualTo("PG1234567C")
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from("86621/65B")),
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from("51072/62R")),
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = personService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).cro).isEqualTo(CROIdentifier.from("86621/65B"))
  }

  @Test
  fun `should find candidate records on soundex matches on first name`() {
    createPerson(
      Person(
        firstName = "Steven",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Micheal",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).firstName).isEqualTo("Steven")
  }

  @Test
  fun `should find candidate records on soundex matches on last name`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smythe",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).lastName).isEqualTo("Smith")
  }

  @Test
  fun `should find candidate records on Levenshtein matches on dob`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1986, 4, 2),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on Levenshtein matches on postcode`() {
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Micheal",
        addresses = listOf(Address(postcode = "ZB5 78O")),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Smith",
      addresses = listOf(Address(postcode = "LS2 1AC"), Address(postcode = "LD2 3BC")),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  private fun first(personEntities: Page<PersonEntity>) =
    personEntities.get().findFirst().get()

  @Test
  fun `should not find candidate records on matching postcode but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        lastName = "Micheal",
        addresses = listOf(Address(postcode = "ZB5 78O")),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    noCandidatesFound(personEntities)
  }

  @Test
  fun `should not find candidate records on matching dob but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1988, 4, 5),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = personService.findCandidateRecords(searchingPerson)

    noCandidatesFound(personEntities)
  }

  private fun noCandidatesFound(personEntities: Page<PersonEntity>) {
    assertThat(personEntities.totalElements).isEqualTo(0)
  }

  private fun createPerson(person: Person): PersonEntity {
    return personRepository.saveAndFlush(PersonEntity.from(person))
  }
}
