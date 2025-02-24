package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate

class SearchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var searchService: SearchService

  @Test
  fun `should find candidate records only in searching in different source systems`() {
    val firstName = randomName()
    val lastName = randomName()
    val personToFind = createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = LIBRA,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = NOMIS,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = DELIUS,
      ),
    )

    stubXHighConfidenceMatches(4)
    stubPersonMatchScore(personToFind.matchId)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(3)
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(PNC, pnc)),
      sourceSystem = COMMON_PLATFORM,
    )
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(PNC, randomPnc())),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPersonWithNewKey(personToFind))

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
  }

  @Test
  fun `should not find candidates which only match on empty PNC`() {
    val personToFind = Person(
      references = listOf(Reference(PNC, "")),
      sourceSystem = COMMON_PLATFORM,
    )
    val person = createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(PNC, "")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(person)

    assertThat(candidateRecords.size).isEqualTo(0)
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      references = listOf(Reference(DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
      sourceSystem = COMMON_PLATFORM,
    )
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(DRIVER_LICENSE_NUMBER, randomDriverLicenseNumber())),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPersonWithNewKey(personToFind))

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should find candidate records with a uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = createPerson(
      Person(
        references = listOf(Reference(DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubXHighConfidenceMatches(2)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should not find candidate records with no uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = createPerson(
      Person(
        references = listOf(Reference(DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        references = listOf(Reference(DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)
    assertThat(candidateRecords.size).isEqualTo(0)
  }

  @Test
  fun `should find candidate records on exact matches on national insurance number`() {
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val personToFind = Person(
      references = listOf(Reference(NATIONAL_INSURANCE_NUMBER, nationalInsuranceNumber)),
      sourceSystem = COMMON_PLATFORM,
    )
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(NATIONAL_INSURANCE_NUMBER, "RF9876543C")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPersonWithNewKey(personToFind))

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(nationalInsuranceNumber)
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val cro = randomCro()
    val personToFind = Person(
      references = listOf(Reference(CRO, cro)),
      sourceSystem = COMMON_PLATFORM,
    )
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(CRO, randomCro())),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPersonWithNewKey(personToFind))

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(CRO).first().identifierValue).isEqualTo(cro)
  }

  @Test
  fun `should find candidate records when multiple reference exact matches on CRO`() {
    val cro = randomCro()
    val searchingPerson = createPersonWithNewKey(
      Person(
        references = listOf(
          Reference(CRO, cro),
          Reference(CRO, randomCro()),
          Reference(CRO, randomCro()),
        ),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    createPersonWithNewKey(
      Person(
        references = listOf(
          Reference(CRO, cro),
          Reference(CRO, randomCro()),
        ),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.firstOrNull { it.identifierValue == cro }?.identifierValue).isEqualTo(cro)
  }

  @Test
  fun `should find candidate records on soundex matches on first name`() {
    val lastName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = "Steven",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = "Micheal",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPersonWithNewKey(
      Person(
        firstName = "Stephen",
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.firstName).isEqualTo("Steven")
  }

  @Test
  fun `should find candidate records on soundex matches on last name`() {
    val firstName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Micheal",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.lastName).isEqualTo("Smith")
  }

  @Test
  fun `should find candidate records on exact match of at least 2 date parts of dob`() {
    val firstName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 2, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on names matches and dob when other record has fields with joins`() {
    val firstName = randomName()
    val lastName = randomName()
    val cro = randomCro()
    val pnc = randomPnc()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        references = listOf(Reference(PNC, pnc), Reference(CRO, cro)),
        addresses = listOf(Address(postcode = randomPostcode())),
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(PNC).size).isEqualTo(0)
    assertThat(candidateRecords[0].candidateRecord.references.getType(CRO).size).isEqualTo(0)
    assertThat(candidateRecords[0].candidateRecord.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on exact match on first 3 chars of postcode`() {
    val firstName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        addresses = listOf(Address(postcode = "LS1 1AB"), Address(postcode = "LD2 3BC")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  @Test
  fun `should find candidate records on exact match on first 3 chars on multiple postcodes`() {
    val firstName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(
          Address(postcode = "LS2 1AB"),
          Address(postcode = "LS3 1AB"),
          Address(postcode = "LS4 1AB"),
        ),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )

    val postcodeWhichWillMatch = Address(postcode = "LS2 3BC")
    val searchingPerson = createPerson(
      Person(
        firstName = firstName,
        lastName = "Smith",
        addresses = listOf(
          Address(postcode = "LS5 1AB"),
          postcodeWhichWillMatch,
        ),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    stubOneHighConfidenceMatch()
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
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Stevenson",
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching dob but not name`() {
    createPerson(
      Person(
        lastName = "Smith",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = "Stephen",
        lastName = "Stevenson",
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching just firstname and lastname`() {
    val firstName = randomName()
    val lastName = randomName()
    createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records when record is marked as merged to another record`() {
    val cro = randomCro()
    val searchingPerson = Person(
      references = listOf(Reference(CRO, cro)),
      sourceSystem = COMMON_PLATFORM,
    )

    val sourcePerson = createPersonWithNewKey(searchingPerson)
    val targetPerson = createPersonWithNewKey(
      Person(
        references = listOf(Reference(CRO, randomCro())),
        sourceSystem = COMMON_PLATFORM,
      ),
    )
    mergeRecord(sourcePerson, targetPerson)

    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPerson(searchingPerson))

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records on matching just firstname and postcode`() {
    val firstName = randomName()
    createPerson(
      Person(
        firstName = firstName,
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = createPerson(
      Person(
        firstName = firstName,
        addresses = listOf(Address(postcode = "LS1 1AB")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records when exclude marker set`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(PNC, pnc)),
      sourceSystem = COMMON_PLATFORM,
    )
    val existingPerson = createPersonWithNewKey(personToFind)
    val excludedRecord = createPersonWithNewKey(personToFind)

    excludeRecord(existingPerson, excludingRecord = excludedRecord)

    stubXHighConfidenceMatches(2)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(excludedRecord)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should order multiple matches descending`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(PNC, pnc)),
      sourceSystem = COMMON_PLATFORM,
    )
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(personToFind)
    createPersonWithNewKey(
      Person(
        references = listOf(Reference(PNC, "1981/0154257C")),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.999999911,
      ),
    )
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(createPersonWithNewKey(personToFind))

    assertThat(candidateRecords.size).isEqualTo(2)
    assertThat(candidateRecords[0].candidateRecord.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(candidateRecords[0].probability).isEqualTo(0.999999911)
    assertThat(candidateRecords[1].candidateRecord.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(candidateRecords[1].probability).isEqualTo(0.9999999)
  }

  @Test
  fun `should log correct number of clusters`() {
    telemetryRepository.deleteAll()
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )
    createPerson(
      Person(
        references = listOf(Reference(CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    stubXHighConfidenceMatches(4)

    val searchingPerson = createPersonWithNewKey(
      Person(
        references = listOf(Reference(CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(searchingPerson)

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "4",
        "UUID_COUNT" to "2",
        "HIGH_CONFIDENCE_COUNT" to "4",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )

    assertThat(candidateRecords.size).isEqualTo(4)
  }

  @Test
  fun `should not find itself`() {
    val record = createPersonWithNewKey(
      Person(
        references = listOf(Reference(PNC, randomPnc())),
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(record)

    noCandidatesFound(candidateRecords)
  }

  private fun noCandidatesFound(records: List<MatchResult>) {
    assertThat(records.size).isEqualTo(0)
  }
}
