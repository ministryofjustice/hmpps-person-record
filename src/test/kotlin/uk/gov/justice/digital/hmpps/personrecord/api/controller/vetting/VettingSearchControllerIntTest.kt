package uk.gov.justice.digital.hmpps.personrecord.api.controller.vetting

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import java.util.UUID

class VettingSearchControllerIntTest : WebTestBase() {

  @Test
  fun `should return matching records`() {
    val personMatchScoreResponse = PersonMatchScore(
      candidateMatchId = UUID.randomUUID().toString(),
      candidateMatchProbability = 0.9999F,
      candidateMatchWeight = JOIN_THRESHOLD + 1,
      candidateShouldJoin = true,
      candidateShouldFracture = false,
    )

    authSetup()
    stubGetRequest(
      url = "/person/search",
      body = jsonMapper.writeValueAsString(personMatchScoreResponse),
    )

    val response = sendGetRequestAsserted<VettingSearchRequest>(
      url = "/person/search",
      roles = listOf(API_READ_ONLY),
      expectedStatus = HttpStatus.OK,
    ).returnResult().responseBody!!
  }
}
