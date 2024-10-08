package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.time.LocalDate

class MatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var matchService: MatchService

  @Test
  fun `should find high confidence match`() {
    val firstName = randomName()
    val lastName = randomName()
    val newRecord = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    val candidateRecords: List<PersonEntity> = listOf(
      PersonEntity.from(newRecord),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, PersonSearchCriteria.from(newRecord))
    assertThat(highConfidenceMatches.size).isEqualTo(1)
    assertThat(highConfidenceMatches[0].probability).isEqualTo(0.9999999)
    assertThat(highConfidenceMatches[0].candidateRecord.firstName).isEqualTo(firstName)
    assertThat(highConfidenceMatches[0].candidateRecord.lastName).isEqualTo(lastName)
  }

  @Test
  fun `should not find high confidence match when score is below threshold`() {
    val firstName = randomName()
    val lastName = randomName()
    val newRecord = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    val candidateRecords: List<PersonEntity> = listOf(
      PersonEntity.from(newRecord),
    )

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.988899))
    stubMatchScore(matchResponse)

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, PersonSearchCriteria.from(newRecord))
    assertThat(highConfidenceMatches.size).isEqualTo(0)
  }

  @Test
  fun `should find multiple high confidence match`() {
    val newRecord1 = Person(
      firstName = randomName(),
      lastName = randomName(),
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    val newRecord2 = Person(
      firstName = randomName(),
      lastName = randomName(),
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.9999991,
      ),
    )
    stubMatchScore(matchResponse)

    val candidateRecords: List<PersonEntity> = listOf(
      PersonEntity.from(newRecord1),
      PersonEntity.from(newRecord2),
    )

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, PersonSearchCriteria.from(newRecord1))
    assertThat(highConfidenceMatches.size).isEqualTo(2)
    assertThat(highConfidenceMatches[0].probability).isEqualTo(0.999999)
    assertThat(highConfidenceMatches[0].candidateRecord.firstName).isEqualTo(newRecord1.firstName)
    assertThat(highConfidenceMatches[1].probability).isEqualTo(0.9999991)
    assertThat(highConfidenceMatches[1].candidateRecord.firstName).isEqualTo(newRecord2.firstName)
  }

  @Test
  fun `should chunk candidates and only send 50 records at a time`() {
    val newRecord = Person(
      firstName = randomName(),
      lastName = randomName(),
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )

    val probabilities = mutableMapOf<String, Double>()
    repeat(50) { index ->
      probabilities[index.toString()] = 0.999999
    }
    val matchResponse = MatchResponse(matchProbabilities = probabilities)
    stubMatchScore(matchResponse)

    val candidateRecords: List<PersonEntity> = generateSequence { PersonEntity.from(newRecord) }.take(100).toList()
    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, PersonSearchCriteria.from(newRecord))

    assertThat(wiremock.findAll(postRequestedFor(urlEqualTo("/person/match"))).size).isEqualTo(2)
    assertThat(highConfidenceMatches.size).isEqualTo(100)
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
