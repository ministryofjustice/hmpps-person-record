package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import java.time.LocalDate

class MatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var matchService: MatchService

  @Test
  fun `should find high confidence match`() {
    val firstName = randomFirstName()
    val lastName = randomLastName()
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

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord)
    assertThat(highConfidenceMatches.size).isEqualTo(1)
    assertThat(highConfidenceMatches[0].probability).isEqualTo(0.9999999)
    assertThat(highConfidenceMatches[0].candidateRecord.firstName).isEqualTo(firstName)
    assertThat(highConfidenceMatches[0].candidateRecord.lastName).isEqualTo(lastName)
  }

  @Test
  fun `should not find high confidence match when score is below threshold`() {
    val firstName = randomFirstName()
    val lastName = randomLastName()
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

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord)
    assertThat(highConfidenceMatches.size).isEqualTo(0)
  }

  @Test
  fun `should find multiple high confidence match and sorted descending`() {
    val newRecord1 = Person(
      firstName = randomFirstName(),
      lastName = randomLastName(),
      dateOfBirth = LocalDate.of(1975, 1, 1),
      sourceSystemType = LIBRA,
    )
    val newRecord2 = Person(
      firstName = randomFirstName(),
      lastName = randomLastName(),
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

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord1)
    assertThat(highConfidenceMatches.size).isEqualTo(2)
    assertThat(highConfidenceMatches[0].probability).isEqualTo(0.9999991)
    assertThat(highConfidenceMatches[0].candidateRecord.firstName).isEqualTo(newRecord2.firstName)
    assertThat(highConfidenceMatches[1].probability).isEqualTo(0.999999)
    assertThat(highConfidenceMatches[1].candidateRecord.firstName).isEqualTo(newRecord1.firstName)
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
