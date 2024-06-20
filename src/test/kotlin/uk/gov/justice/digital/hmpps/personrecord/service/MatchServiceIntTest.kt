package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import java.time.LocalDate
import java.util.*


class MatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var matchService: MatchService

  @Test
  fun `should find high confidence match`() {
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val dob = LocalDate.of(1975, 1, 1)
    val newRecord = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dob,
      sourceSystemType = LIBRA,
    )
    val candidateRecords: List<PersonEntity> = listOf(
      PersonEntity.from(newRecord),
    )

    val spyUuid = spy(UUID::class)
    val fixedUuid = UUID.fromString("123e4567-e89b-12d3-a456-556642440000")
    doReturn(fixedUuid).whenever(spyUuid)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf(fixedUuid.toString() to 0.9999999))
    stubMatchScore(matchResponse)

    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord)
    assertThat(highConfidenceMatches.size).isEqualTo(1)
    assertThat(highConfidenceMatches[0].probability).isEqualTo("0.9999999")
    assertThat(highConfidenceMatches[0].candidateRecord.firstName).isEqualTo(firstName)
    assertThat(highConfidenceMatches[0].candidateRecord.lastName).isEqualTo(lastName)
  }

//  @Test
//  fun `should not find high confidence match when score is below threshold`() {
//    val firstName = randomFirstName()
//    val lastName = randomLastName()
//    val dob = LocalDate.of(1975, 1, 1)
//    val newRecord = Person(
//      firstName = firstName,
//      lastName = lastName,
//      dateOfBirth = dob,
//      sourceSystemType = LIBRA,
//    )
//    val candidateRecords: List<PersonEntity> = listOf(
//      PersonEntity.from(newRecord),
//    )
//
//    val matchRequest = createMatchRequest(candidateRecords, matchingPerson = newRecord)
//    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf(newRecord.))
//    stubMatchScore(matchRequest, matchResponse)
//
//    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord)
//    assertThat(highConfidenceMatches.size).isEqualTo(0)
//  }

//  @Test
//  fun `should find multiple high confidence match and sorted descending`() {
//    val firstName1 = randomFirstName()
//    val lastName1 = randomLastName()
//    val dob1 = LocalDate.of(1975, 1, 1)
//    val newRecord1 = Person(
//      firstName = firstName1,
//      lastName = lastName1,
//      dateOfBirth = dob1,
//      sourceSystemType = LIBRA,
//    )
//
//    val matchRequest1 = createMatchRequest(newRecord1, matchingPerson = newRecord1)
//    val matchResponse1 = MatchResponse(matchProbability = MatchResponseData("0.999999"))
//    stubMatchScore(matchRequest1, matchResponse1)
//
//    val firstName2 = randomFirstName()
//    val lastName2 = randomLastName()
//    val dob2 = LocalDate.of(1975, 1, 1)
//    val newRecord2 = Person(
//      firstName = firstName2,
//      lastName = lastName2,
//      dateOfBirth = dob2,
//      sourceSystemType = LIBRA,
//    )
//    val matchRequest2 = createMatchRequest(newRecord2, matchingPerson = newRecord1)
//    val matchResponse2 = MatchResponse(matchProbability = MatchResponseData("0.9999991"))
//    stubMatchScore(matchRequest2, matchResponse2)
//
//    val candidateRecords: List<PersonEntity> = listOf(
//      PersonEntity.from(newRecord1),
//      PersonEntity.from(newRecord2),
//    )
//
//    val highConfidenceMatches = matchService.findHighConfidenceMatches(candidateRecords, newRecord1)
//    assertThat(highConfidenceMatches.size).isEqualTo(2)
//    assertThat(highConfidenceMatches[0].probability).isEqualTo("0.9999991")
//    assertThat(highConfidenceMatches[1].probability).isEqualTo("0.999999")
//  }

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
