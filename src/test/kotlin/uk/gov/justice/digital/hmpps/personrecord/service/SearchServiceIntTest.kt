package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.time.LocalDate

class SearchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var searchService: SearchService

  @Autowired
  private lateinit var personRepository: PersonRepository

  @Test
  fun `should find candidate records only in searching source system`() {
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val personToFind = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = NOMIS,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = DELIUS,
      ),
    )

    val personEntities = searchService.findCandidateRecords(personToFind)
    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val pnc = randomPnc()
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pnc)),
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = searchService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).pnc).isEqualTo(PNCIdentifier.from(pnc))
  }

  @Test
  fun `should not find candidates which only match on empty PNC`() {
    val firstName = randomFirstName()
    val personToFind = Person(
      firstName = firstName,
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

    val personEntities = searchService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).firstName).isEqualTo(firstName)
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      driverLicenseNumber = driverLicenseNumber,
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = "0987654321",
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = searchService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).driverLicenseNumber).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should find candidate records on exact matches on national insurance number`() {
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val personToFind = Person(
      nationalInsuranceNumber = nationalInsuranceNumber,
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        nationalInsuranceNumber = "RF9876543C",
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = searchService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).nationalInsuranceNumber).isEqualTo(nationalInsuranceNumber)
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val cro = randomCro()
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from(cro)),
      sourceSystemType = HMCTS,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from("51072/62R")),
        sourceSystemType = HMCTS,
      ),
    )

    val personEntities = searchService.findCandidateRecords(personToFind)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).cro).isEqualTo(CROIdentifier.from(cro))
  }

  @Test
  fun `should find candidate records on soundex matches on first name`() {
    val lastName = randomLastName()
    createPerson(
      Person(
        firstName = "Steven",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = "Micheal",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).firstName).isEqualTo("Steven")
  }

  @Test
  fun `should find candidate records on soundex matches on last name`() {
    val firstName = randomFirstName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smythe",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).lastName).isEqualTo("Smith")
  }

  @Test
  fun `should find candidate records on Levenshtein matches on dob`() {
    val firstName = randomFirstName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on Levenshtein matches on postcode`() {
    val firstName = randomFirstName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = HMCTS,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      addresses = listOf(Address(postcode = "LS2 1AC"), Address(postcode = "LD2 3BC")),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

    assertThat(personEntities.totalElements).isEqualTo(1)
    assertThat(first(personEntities).addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  @Test
  fun `should not find candidate records on matching postcode but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = HMCTS,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

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

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = HMCTS,
    )
    val personEntities = searchService.findCandidateRecords(searchingPerson)

    noCandidatesFound(personEntities)
  }

  private fun noCandidatesFound(personEntities: Page<PersonEntity>) {
    assertThat(personEntities.totalElements).isEqualTo(0)
  }

  private fun createPerson(person: Person): PersonEntity = personRepository.saveAndFlush(PersonEntity.from(person))

  private fun first(personEntities: Page<PersonEntity>) =
    personEntities.get().findFirst().get()
}
