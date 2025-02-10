package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
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
    val candidateRecords = searchService.findCandidateRecordsWithUuid(personToFind)

    assertThat(candidateRecords.size).isEqualTo(3)
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
