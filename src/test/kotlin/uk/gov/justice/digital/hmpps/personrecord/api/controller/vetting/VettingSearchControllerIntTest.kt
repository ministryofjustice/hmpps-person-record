package uk.gov.justice.digital.hmpps.personrecord.api.controller.vetting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingMatchStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class VettingSearchControllerIntTest : WebTestBase() {

  @Nested
  inner class Success {

    @Test
    fun `single cluster - should return correct search result`() {
      val prisonNumber1 = randomPrisonNumber()
      val prisonNumber2 = randomPrisonNumber()
      val cluster = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber2))
      val strongestMatchPersonEntity = cluster.personEntities.first { it.prisonNumber == prisonNumber1 }
      val weakestMatchPersonEntity = cluster.personEntities.first { it.prisonNumber == prisonNumber2 }

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = strongestMatchPersonEntity.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = weakestMatchPersonEntity.matchId.toString(),
          candidateMatchProbability = 0.8888F,
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

      val strongestPersonPrimaryPseudonym = strongestMatchPersonEntity.pseudonyms.first { it.nameType == NameType.PRIMARY }
      val vettingSearchResponse = sendPostRequestAsserted<VettingSearchResponse>(
        url = "/person/search",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingSearchRequest(
          fullName = """${strongestPersonPrimaryPseudonym.firstName} ${strongestPersonPrimaryPseudonym.middleNames} ${strongestPersonPrimaryPseudonym.lastName}""",
          dateOfBirth = strongestPersonPrimaryPseudonym.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(vettingSearchResponse.data).hasSize(1)
      assertThat(vettingSearchResponse.data.first().linkedRecords).hasSize(1)

      val strongestPersonFromResponse = vettingSearchResponse.data.first()
      assertThat(strongestPersonFromResponse.name.firstName).isEqualTo(strongestPersonPrimaryPseudonym.firstName)
      assertThat(strongestPersonFromResponse.name.middleNames).isEqualTo(strongestPersonPrimaryPseudonym.middleNames)
      assertThat(strongestPersonFromResponse.name.lastName).isEqualTo(strongestPersonPrimaryPseudonym.lastName)
      assertThat(strongestPersonFromResponse.name.dateOfBirth).isEqualTo(strongestPersonPrimaryPseudonym.dateOfBirth)
      assertThat(strongestPersonFromResponse.sourceSystem).isEqualTo(strongestMatchPersonEntity.sourceSystem)
      assertThat(strongestPersonFromResponse.status).isEqualTo(VettingMatchStatus.HIGH_CONFIDENCE_MATCH)
      assertThat(strongestPersonFromResponse.aliases).usingRecursiveComparison().isEqualTo(strongestMatchPersonEntity.getAliases().map { Alias.from(it) })
      assertThat(strongestPersonFromResponse.identifiers).usingRecursiveComparison().isEqualTo(strongestMatchPersonEntity.references.map { Reference.from(it) })
      assertThat(strongestPersonFromResponse.addresses).hasSize(strongestMatchPersonEntity.addresses.size)

      val weakestPersonFromResponse = vettingSearchResponse.data.first().linkedRecords.first()
      val weakestPersonPrimaryPseudonym = weakestMatchPersonEntity.pseudonyms.first { it.nameType == NameType.PRIMARY }
      assertThat(weakestPersonFromResponse.name.firstName).isEqualTo(weakestPersonPrimaryPseudonym.firstName)
      assertThat(weakestPersonFromResponse.name.middleNames).isEqualTo(weakestPersonPrimaryPseudonym.middleNames)
      assertThat(weakestPersonFromResponse.name.lastName).isEqualTo(weakestPersonPrimaryPseudonym.lastName)
      assertThat(weakestPersonFromResponse.name.dateOfBirth).isEqualTo(weakestPersonPrimaryPseudonym.dateOfBirth)
      assertThat(weakestPersonFromResponse.sourceSystem).isEqualTo(weakestMatchPersonEntity.sourceSystem)
      assertThat(weakestPersonFromResponse.status).isEqualTo(VettingMatchStatus.HIGH_CONFIDENCE_MATCH)
      assertThat(weakestPersonFromResponse.aliases).usingRecursiveComparison().isEqualTo(weakestMatchPersonEntity.getAliases().map { Alias.from(it) })
      assertThat(weakestPersonFromResponse.identifiers).usingRecursiveComparison().isEqualTo(weakestMatchPersonEntity.references.map { Reference.from(it) })
      assertThat(weakestPersonFromResponse.addresses).hasSize(weakestMatchPersonEntity.addresses.size)
    }

    @Test
    fun `multi cluster - should return correct search result`() {
      val prisonNumber1 = randomPrisonNumber()
      val prisonNumber2 = randomPrisonNumber()
      val prisonNumber3 = randomPrisonNumber()
      val prisonNumber4 = randomPrisonNumber()
      val cluster1 = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber2))
      val cluster2 = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumber3))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber4))
      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber2 }
      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber3 }
      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = strongestPersonFromCluster2.matchId.toString(),
          candidateMatchProbability = 0.8888F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = weakestPersonFromCluster2.matchId.toString(),
          candidateMatchProbability = 0.6666F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = strongestPersonFromCluster1.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = JOIN_THRESHOLD + 1,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = weakestPersonFromCluster1.matchId.toString(),
          candidateMatchProbability = 0.7777F,
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

      val search = strongestPersonFromCluster1.pseudonyms.first { it.nameType == NameType.PRIMARY }
      val vettingSearchResponse = sendPostRequestAsserted<VettingSearchResponse>(
        url = "/person/search",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingSearchRequest(
          fullName = """${search.firstName} ${search.middleNames} ${search.lastName}""",
          dateOfBirth = search.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(vettingSearchResponse.data).hasSize(2)
      assertThat(vettingSearchResponse.data.first().name.firstName).isEqualTo(strongestPersonFromCluster1.getPrimaryName().firstName)
      assertThat(vettingSearchResponse.data.first().linkedRecords).hasSize(1)

      assertThat(vettingSearchResponse.data.last().name.firstName).isEqualTo(strongestPersonFromCluster2.getPrimaryName().firstName)
      assertThat(vettingSearchResponse.data.last().linkedRecords).hasSize(1)
    }

    @Test
    fun `no matches found - should return empty list`() {
      authSetup()
      stubPostRequest(
        url = "/person/search",
        responseBody = jsonMapper.writeValueAsString(emptyList<PersonMatchScore>()),
      )

      val vettingSearchResponse = sendPostRequestAsserted<VettingSearchResponse>(
        url = "/person/search",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingSearchRequest(
          fullName = """${randomLowerCaseString()} ${randomLowerCaseString()} ${randomLowerCaseString()}""",
          dateOfBirth = randomDate(),
        ),
      ).returnResult().responseBody!!

      assertThat(vettingSearchResponse.data).isEmpty()
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
