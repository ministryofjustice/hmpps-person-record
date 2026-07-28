package uk.gov.justice.digital.hmpps.personrecord.api.controller.vetting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

class VettingSearchControllerIntTest : WebTestBase() {

  @Test
  fun `should return strongest matching person`() {
    val cluster = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails())
      .addPerson(createRandomPrisonPersonDetails())
    val strongestMatchPerson = cluster.personEntities.first()
    val primaryPseudonym = strongestMatchPerson.pseudonyms.first { it.nameType == NameType.PRIMARY }

    val personMatchScoreResponse = PersonMatchScore(
      candidateMatchId = strongestMatchPerson.matchId.toString(),
      candidateMatchProbability = 0.9999F,
      candidateMatchWeight = JOIN_THRESHOLD + 1,
      candidateShouldJoin = true,
      candidateShouldFracture = false,
    )

    authSetup()
    stubPostRequest(
      url = "/person/search",
      responseBody = jsonMapper.writeValueAsString(listOf(personMatchScoreResponse)),
    )

    val responseBody = sendPostRequestAsserted<VettingSearchResponse>(
      url = "/person/search",
      roles = listOf(API_READ_ONLY),
      expectedStatus = HttpStatus.OK,
      body = VettingSearchRequest(
        fullName = """${primaryPseudonym.firstName} ${primaryPseudonym.middleNames} ${primaryPseudonym.lastName}""",
        dateOfBirth = primaryPseudonym.dateOfBirth!!,
      ),
    ).returnResult().responseBody!!

    assertThat(responseBody.name.firstName).isEqualTo(primaryPseudonym.firstName)
  }
}
