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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class VettingSearchControllerIntTest : WebTestBase() {

  @Nested
  inner class Success {

    @Test
    fun `should return strongest matching person`() {
      val prisonNumber1 = randomPrisonNumber()
      val prisonNumber2 = randomPrisonNumber()
      val cluster = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber2))
      val strongestMatchPerson = cluster.personEntities.first { it.prisonNumber == prisonNumber1 }
      val weakestMatchPerson = cluster.personEntities.first { it.prisonNumber == prisonNumber2 }

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = strongestMatchPerson.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = weakestMatchPerson.matchId.toString(),
          candidateMatchProbability = 0.9888F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
      )

      authSetup()
      stubPostRequest(
        url = "/person/search",
        responseBody = jsonMapper.writeValueAsString(personMatchScores),
      )

      val strongestPersonPrimaryPseudonym = strongestMatchPerson.pseudonyms.first { it.nameType == NameType.PRIMARY }
      val responseBody = sendPostRequestAsserted<VettingSearchResponse>(
        url = "/person/search",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingSearchRequest(
          fullName = """${strongestPersonPrimaryPseudonym.firstName} ${strongestPersonPrimaryPseudonym.middleNames} ${strongestPersonPrimaryPseudonym.lastName}""",
          dateOfBirth = strongestPersonPrimaryPseudonym.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!

      assertThat(responseBody.name.firstName).isEqualTo(strongestPersonPrimaryPseudonym.firstName)
      assertThat(responseBody.name.middleNames).isEqualTo(strongestPersonPrimaryPseudonym.middleNames)
      assertThat(responseBody.name.lastName).isEqualTo(strongestPersonPrimaryPseudonym.lastName)
      assertThat(responseBody.name.dateOfBirth).isEqualTo(strongestPersonPrimaryPseudonym.dateOfBirth)

      val linkedRecord = responseBody.linkedRecords.first()
      val weakestPersonPrimaryPseudonym = weakestMatchPerson.pseudonyms.first { it.nameType == NameType.PRIMARY }
      assertThat(linkedRecord.name.firstName).isEqualTo(weakestPersonPrimaryPseudonym.firstName)
      assertThat(linkedRecord.name.middleNames).isEqualTo(weakestPersonPrimaryPseudonym.middleNames)
      assertThat(linkedRecord.name.lastName).isEqualTo(weakestPersonPrimaryPseudonym.lastName)
      assertThat(linkedRecord.name.dateOfBirth).isEqualTo(weakestPersonPrimaryPseudonym.dateOfBirth)
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
