package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.time.LocalDate

class SearchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var searchService: SearchService

  @Autowired
  private lateinit var personRepository: PersonRepository

  @Autowired
  private lateinit var personKeyRepository: PersonKeyRepository

  @Test
  fun `should find candidate records only in searching source system`() {
    val firstName = randomName()
    val lastName = randomName()
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
        sourceSystemType = COMMON_PLATFORM,
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

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should find candidate records only in searching in different source systems`() {
    val firstName = randomName()
    val lastName = randomName()
    val personToFind = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    createPersonWithUuid(personToFind)
    createPersonWithUuid(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPersonWithUuid(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = NOMIS,
      ),
    )
    createPersonWithUuid(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = DELIUS,
      ),
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.9999999,
        "2" to 0.9999999,
        "3" to 0.9999999,
      ),
    )
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(4)
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val pnc = randomPnc()
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pnc)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(randomPnc())),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.pnc).isEqualTo(PNCIdentifier.from(pnc))
  }

  @Test
  fun `should not find candidates which only match on empty PNC`() {
    val firstName = randomName()
    val personToFind = Person(
      firstName = firstName,
      lastName = randomName(),
      dateOfBirth = LocalDate.of(1975, 2, 1),
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("")),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        dateOfBirth = LocalDate.of(1975, 2, 1),
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.firstName).isEqualTo(firstName)
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      driverLicenseNumber = driverLicenseNumber,
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = randomDriverLicenseNumber(),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.driverLicenseNumber).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should find candidate records with a uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      driverLicenseNumber = driverLicenseNumber,
      sourceSystemType = COMMON_PLATFORM,
    )
    createPersonWithUuid(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = driverLicenseNumber,
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999, "1" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.driverLicenseNumber).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should not find candidate records with no uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      driverLicenseNumber = driverLicenseNumber,
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        driverLicenseNumber = driverLicenseNumber,
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)
    assertThat(candidateRecords.size).isEqualTo(0)
  }

  @Test
  fun `should find candidate records on exact matches on national insurance number`() {
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val personToFind = Person(
      nationalInsuranceNumber = nationalInsuranceNumber,
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        nationalInsuranceNumber = "RF9876543C",
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.nationalInsuranceNumber).isEqualTo(nationalInsuranceNumber)
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val cro = randomCro()
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from(cro)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(croIdentifier = CROIdentifier.from(randomCro())),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.cro).isEqualTo(CROIdentifier.from(cro))
  }

  @Test
  fun `should find candidate records on soundex matches on first name`() {
    val lastName = randomName()
    createPerson(
      Person(
        firstName = "Steven",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        firstName = "Micheal",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = COMMON_PLATFORM,
    )
    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.firstName).isEqualTo("Steven")
  }

  @Test
  fun `should find candidate records on soundex matches on last name`() {
    val firstName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smythe",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.lastName).isEqualTo("Smith")
  }

  @Test
  fun `should find candidate records on Levenshtein matches on dob`() {
    val firstName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1975, 2, 1),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on Levenshtein matches on postcode`() {
    val firstName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      addresses = listOf(Address(postcode = "LS2 1AB"), Address(postcode = "LD2 3BC")),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  @Test
  fun `should find candidate records on Levenshtein matches on multiple postcodes`() {
    val firstName = randomName()
    createPersonWithUuid(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(
          Address(postcode = "LS2 1AB"),
          Address(postcode = "LS3 1AB"),
          Address(postcode = "LS4 1AB"),
        ),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPersonWithUuid(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      addresses = listOf(
        Address(postcode = "LS5 1AB"),
        Address(postcode = "LD2 3BC"),
      ),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.addresses[0].postcode).isEqualTo("LS2 1AB")
    assertThat(candidateRecords[0].candidateRecord.addresses[1].postcode).isEqualTo("LS3 1AB")
    assertThat(candidateRecords[0].candidateRecord.addresses[2].postcode).isEqualTo("LS4 1AB")
  }

  @Test
  fun `should not find candidate records on matching postcode but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching dob but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = "Stephen",
      lastName = "Stevenson",
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching just firstname and lastname`() {
    val firstName = randomName()
    val lastName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = lastName,
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching just firstname and postcode`() {
    val firstName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should order multiple matches descending`() {
    val pnc = randomPnc()
    val personToFind = Person(
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pnc)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(personToFind)
    createPerson(
      Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("1981/0154257C")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.999999911,
      ),
    )
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(2)
    assertThat(candidateRecords[0].candidateRecord.pnc).isEqualTo(PNCIdentifier.from(pnc))
    assertThat(candidateRecords[0].probability).isEqualTo(0.999999911)
    assertThat(candidateRecords[1].candidateRecord.pnc).isEqualTo(PNCIdentifier.from(pnc))
    assertThat(candidateRecords[1].probability).isEqualTo(0.9999999)
  }

  private fun noCandidatesFound(records: List<MatchResult>) {
    assertThat(records.size).isEqualTo(0)
  }

  private fun createPerson(person: Person): PersonEntity = personRepository.saveAndFlush(PersonEntity.from(person))

  private fun createPersonWithUuid(person: Person): PersonEntity {
    val personKeyEntity = personKeyRepository.saveAndFlush(PersonKeyEntity.new())
    val personEntity = createPerson(person)
    personEntity.personKey = personKeyEntity
    return personRepository.saveAndFlush(personEntity)
  }

  private fun stubMatchScore(matchResponse: MatchResponse) {
    wiremock.stubFor(
      WireMock.post("/person/match")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }
}
