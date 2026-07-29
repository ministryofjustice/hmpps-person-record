package uk.gov.justice.digital.hmpps.personrecord.api.controller.vetting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

class VettingSearchControllerIntTest : WebTestBase() {

  @Nested
  inner class Success {

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
      assertThat(responseBody.name.middleNames).isEqualTo(primaryPseudonym.middleNames)
      assertThat(responseBody.name.lastName).isEqualTo(primaryPseudonym.lastName)
      assertThat(responseBody.name.dateOfBirth).isEqualTo(primaryPseudonym.dateOfBirth)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = "/person/search",
        body = VettingSearchRequest(
          fullName = """${randomLowerCaseString()} ${randomLowerCaseString()} ${randomLowerCaseString()}""",
          dateOfBirth = randomDate(),
        ),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = "/person/search",
        body = VettingSearchRequest(
          fullName = """${randomLowerCaseString()} ${randomLowerCaseString()} ${randomLowerCaseString()}""",
          dateOfBirth = randomDate(),
        ),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }
}
