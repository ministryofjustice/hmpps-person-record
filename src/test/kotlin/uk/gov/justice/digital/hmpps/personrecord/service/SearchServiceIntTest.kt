package uk.gov.justice.digital.hmpps.personrecord.service

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
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
    val personToFind = createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = LIBRA,
      ),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = NOMIS,
      ),
      personKeyEntity = createPersonKey(),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = DELIUS,
      ),
      personKeyEntity = createPersonKey(),
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

    assertThat(candidateRecords.size).isEqualTo(3)
  }

  @Test
  fun `should find candidate records on exact matches on PNC`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.PNC, pnc)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.PNC, randomPnc())),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
  }

  @Test
  fun `should not find candidates which only match on empty PNC`() {
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.PNC, "")),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.PNC, "")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(0)
  }

  @Test
  fun `should find candidate records on exact matches on driver license number`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, randomDriverLicenseNumber())),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should find candidate records with a uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = createPerson(
      Person(
        references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999, "1" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
  }

  @Test
  fun `should not find candidate records with no uuid`() {
    val driverLicenseNumber = randomDriverLicenseNumber()
    val personToFind = createPerson(
      Person(
        references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.DRIVER_LICENSE_NUMBER, driverLicenseNumber)),
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
      references = listOf(Reference(IdentifierType.NATIONAL_INSURANCE_NUMBER, nationalInsuranceNumber)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.NATIONAL_INSURANCE_NUMBER, "RF9876543C")),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(nationalInsuranceNumber)
  }

  @Test
  fun `should find candidate records on exact matches on CRO`() {
    val cro = randomCro()
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.CRO, cro)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, randomCro())),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(personToFind)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
  }

  @Test
  fun `should find candidate records when multiple reference exact matches on CRO`() {
    val cro = randomCro()
    val searchingPerson = Person(
      references = listOf(
        Reference(IdentifierType.CRO, cro),
        Reference(IdentifierType.CRO, randomCro()),
        Reference(IdentifierType.CRO, randomCro()),
      ),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(
      Person(
        references = listOf(
          Reference(IdentifierType.CRO, cro),
          Reference(IdentifierType.CRO, randomCro()),
        ),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.firstOrNull { it.identifierValue == cro }?.identifierValue).isEqualTo(cro)
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
  fun `should find candidate records on exact match of at least 2 date parts of dob`() {
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
  fun `should find candidate records on names matches and dob when other record has fields with joins`() {
    val firstName = randomName()
    val lastName = randomName()
    val cro = randomCro()
    val pnc = randomPnc()
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sourceSystemType = COMMON_PLATFORM,
      ),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = lastName,
      references = listOf(Reference(IdentifierType.PNC, pnc), Reference(IdentifierType.CRO, cro)),
      addresses = listOf(Address(postcode = randomPostcode())),
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.PNC).size).isEqualTo(0)
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.CRO).size).isEqualTo(0)
    assertThat(candidateRecords[0].candidateRecord.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
  }

  @Test
  fun `should find candidate records on exact match on first 3 chars of postcode`() {
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
      addresses = listOf(Address(postcode = "LS1 1AB"), Address(postcode = "LD2 3BC")),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    assertThat(candidateRecords.size).isEqualTo(1)
    assertThat(candidateRecords[0].candidateRecord.addresses[0].postcode).isEqualTo("LS1 1AB")
  }

  @Test
  fun `should find candidate records on exact match on first 3 chars on multiple postcodes`() {
    val firstName = randomName()
    createPerson(
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
      personKeyEntity = createPersonKey(),
    )
    createPerson(
      Person(
        firstName = firstName,
        lastName = "Smythe",
        addresses = listOf(Address(postcode = "PR7 3DU")),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )

    val postcodeWhichWillMatch = Address(postcode = "LS2 3BC")
    val searchingPerson = Person(
      firstName = firstName,
      lastName = "Smith",
      addresses = listOf(
        Address(postcode = "LS5 1AB"),
        postcodeWhichWillMatch,
      ),
      sourceSystemType = COMMON_PLATFORM,
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

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
      personKeyEntity = createPersonKey(),
    )

    val searchingPerson = Person(
      firstName = firstName,
      lastName = lastName,
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records when record is marked as merged to another record`() {
    val cro = randomCro()
    val searchingPerson = Person(
      references = listOf(Reference(IdentifierType.CRO, cro)),
      sourceSystemType = COMMON_PLATFORM,
    )

    val sourcePerson = createPerson(searchingPerson, personKeyEntity = createPersonKey())
    val targetPerson = createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, randomCro())),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )
    mergeRecord(sourcePerson, targetPerson)

    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

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
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should not find candidate records when exclude marker set`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.PNC, pnc)),
      sourceSystemType = COMMON_PLATFORM,
    )
    val existingPerson = createPerson(personToFind, personKeyEntity = createPersonKey())
    val excludedRecord = createPerson(personToFind, personKeyEntity = createPersonKey())

    excludeRecord(existingPerson, excludingRecord = excludedRecord)

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.9999999,
      ),
    )
    stubMatchScore(matchResponse)
    val candidateRecords = searchService.findCandidateRecordsWithUuid(excludedRecord)

    noCandidatesFound(candidateRecords)
  }

  @Test
  fun `should order multiple matches descending`() {
    val pnc = randomPnc()
    val personToFind = Person(
      references = listOf(Reference(IdentifierType.PNC, pnc)),
      sourceSystemType = COMMON_PLATFORM,
    )
    createPerson(personToFind)
    createPerson(personToFind)
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.PNC, "1981/0154257C")),
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
    assertThat(candidateRecords[0].candidateRecord.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(candidateRecords[0].probability).isEqualTo(0.999999911)
    assertThat(candidateRecords[1].candidateRecord.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(candidateRecords[1].probability).isEqualTo(0.9999999)
  }

  @Test
  fun `should log correct number of clusters`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
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

    val searchingPerson = Person(
      references = listOf(Reference(IdentifierType.CRO, cro)),
      sourceSystemType = COMMON_PLATFORM,
    )
    val candidateRecords = searchService.findCandidateRecordsBySourceSystem(searchingPerson)

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
  fun `should not find its self`() {
    val record = createPerson(
      Person(
        references = listOf(Reference(IdentifierType.PNC, randomPnc())),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )

    val candidateRecords = searchService.findCandidateRecordsWithUuid(record)

    noCandidatesFound(candidateRecords)
  }

  private fun noCandidatesFound(records: List<MatchResult>) {
    assertThat(records.size).isEqualTo(0)
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
