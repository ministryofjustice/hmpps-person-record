package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class CommonPlatformCandidateSearchControllerIntTest : WebTestBase() {

  @Nested
  inner class Success {

    @Test
    fun `should return probation candidate matches only`() {
      val commonPlatformRecord = createPersonWithNewKey(createRandomCommonPlatformPersonDetails())
      val probationRecord = createPersonWithNewKey(createRandomProbationPersonDetails())
      val prisonRecord = createPersonWithNewKey(createRandomPrisonPersonDetails())

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = commonPlatformRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 14.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = probationRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 16.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = prisonRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 15.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
      )

      authSetup()
      stubGetRequest(
        url = "/person/search/${commonPlatformRecord.matchId}",
        body = jsonMapper.writeValueAsString(personMatchScores),
      )

      val candidates = sendGetRequestAsserted<List<CanonicalRecord>>(
        url = candidateSearchUrl(commonPlatformRecord.defendantId!!),
        roles = listOf(Roles.API_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(candidates).hasSize(1)
      assertThat(candidates[0].identifiers.crns.first()).isEqualTo(probationRecord.crn)
    }

    @Test
    fun `should return no candidate matches if no probation candidates are found`() {
      val commonPlatformRecord = createPersonWithNewKey(createRandomCommonPlatformPersonDetails())
      val prisonRecord = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val libraRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = commonPlatformRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 14.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = libraRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 16.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = prisonRecord.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 15.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
      )

      authSetup()
      stubGetRequest(
        url = "/person/search/${commonPlatformRecord.matchId}",
        body = jsonMapper.writeValueAsString(personMatchScores),
      )

      val candidates = sendGetRequestAsserted<List<CanonicalRecord>>(
        url = candidateSearchUrl(commonPlatformRecord.defendantId!!),
        roles = listOf(Roles.API_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(candidates).isEmpty()
    }

    @Test
    fun `should return no candidate matches when defendant Id does not exist`() {
      val candidates = sendGetRequestAsserted<List<CanonicalRecord>>(
        url = candidateSearchUrl(randomDefendantId()),
        roles = listOf(Roles.API_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(candidates).isEmpty()
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = candidateSearchUrl(randomDefendantId()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = candidateSearchUrl(randomDefendantId()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun candidateSearchUrl(defendantId: String): String = "/person/commonplatform/$defendantId/candidate-matches"
}
