package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class PersonMatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  @Test
  fun `should find one high confidence match`() {
    val searchingRecord = createPersonWithNewKey(createExamplePerson())
    val foundRecord = createPersonWithNewKey(createExamplePerson())

    stubOnePersonMatchHighConfidenceMatch(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

    val highConfidenceMatch = personMatchService.findHighestConfidencePersonRecord(searchingRecord)
    assertThat(highConfidenceMatch?.matchId).isEqualTo(foundRecord.matchId)
  }

  @Test
  fun `should find multiple high confidence match`() {
    val searchingRecord = createPersonWithNewKey(createExamplePerson())

    val foundRecords = listOf(
      createPersonWithNewKey(createExamplePerson()),
      createPersonWithNewKey(createExamplePerson()),
      createPersonWithNewKey(createExamplePerson()),
    )

    stubXPersonMatchHighConfidenceMatches(
      matchId = searchingRecord.matchId,
      results = foundRecords.map { it.matchId },
    )

    val highConfidenceMatch = personMatchService.findHighestConfidencePersonRecord(searchingRecord)
    assertThat(highConfidenceMatch?.matchId).isEqualTo(foundRecords[0].matchId)
  }

  @Test
  fun `should not return low confidence match`() {
    val searchingRecord = createPersonWithNewKey(createExamplePerson())
    val lowConfidenceRecord = createPersonWithNewKey(createExamplePerson())

    stubOnePersonMatchLowConfidenceMatch(
      matchId = searchingRecord.matchId,
      matchedRecord = lowConfidenceRecord.matchId,
    )

    val highConfidenceMatch = personMatchService.findHighestConfidencePersonRecord(searchingRecord)

    noCandidateFound(highConfidenceMatch)
  }

  @Test
  fun `should not return high confidence match that has no UUID`() {
    val searchingRecord = createPerson(createExamplePerson())
    val foundRecord = createPerson(createExamplePerson())

    stubOnePersonMatchHighConfidenceMatch(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

    val highConfidenceMatch = personMatchService.findHighestConfidencePersonRecord(searchingRecord)

    noCandidateFound(highConfidenceMatch)
  }

  @Test
  fun `should not find candidate records when exclude marker set`() {
    val searchingRecord = createPersonWithNewKey(createExamplePerson())
    val excludedRecord = createPersonWithNewKey(createExamplePerson())

    excludeRecord(searchingRecord, excludingRecord = excludedRecord)
    awaitAssert { assertThat(personRepository.findByMatchId(searchingRecord.matchId)?.overrideMarkers?.size).isEqualTo(1) }

    stubOnePersonMatchHighConfidenceMatch(matchId = searchingRecord.matchId, matchedRecord = excludedRecord.matchId)

    val highConfidenceMatch = personMatchService.findHighestConfidencePersonRecord(searchingRecord)

    noCandidateFound(highConfidenceMatch)
  }

  private fun noCandidateFound(highConfidenceMatch: PersonEntity?) {
    assertThat(highConfidenceMatch).isNull()
  }

  private fun createExamplePerson() = Person(
    firstName = randomName(),
    lastName = randomName(),
    dateOfBirth = randomDate(),
    sourceSystem = LIBRA,
  )
}
