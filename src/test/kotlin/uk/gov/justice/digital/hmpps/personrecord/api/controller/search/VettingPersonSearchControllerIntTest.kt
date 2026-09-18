package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_VETTING_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.CanonicalSearchIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class VettingPersonSearchControllerIntTest : WebTestBase() {

  @Nested
  inner class Success {

    @Test
    fun `single cluster - should return correct search result`() {
      val prisonNumber1 = randomPrisonNumber()
      val prisonNumber2 = randomPrisonNumber()
      val cluster = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber2))
      val personEntity1 = cluster.personEntities.first { it.prisonNumber == prisonNumber1 }
      val personEntity2 = cluster.personEntities.first { it.prisonNumber == prisonNumber2 }

      val personMatchScores = listOf(
        PersonMatchScore(
          candidateMatchId = personEntity1.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 24.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
        PersonMatchScore(
          candidateMatchId = personEntity2.matchId.toString(),
          candidateMatchProbability = 0.9999F,
          candidateMatchWeight = 90.0000F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        ),
      )

      authSetup()
      stubPostRequest(
        url = "/person/search",
        responseBody = jsonMapper.writeValueAsString(personMatchScores),
      )

      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = personEntity1.getPrimaryName().firstName!!,
          lastName = personEntity1.getPrimaryName().lastName!!,
          dateOfBirth = personEntity1.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.clusters).hasSize(1) // results span only 1 cluster
      assertThat(personSearchResponse.clusters.first().results).hasSize(2) // the cluster has 2 records on it

      val searchResult1 = personSearchResponse.clusters.first().results.first()
      assertThat(searchResult1.name.firstName).isEqualTo(personEntity1.getPrimaryName().firstName!!)
      assertThat(searchResult1.name.lastName).isEqualTo(personEntity1.getPrimaryName().lastName!!)
      assertThat(searchResult1.name.dateOfBirth).isEqualTo(personEntity1.getPrimaryName().dateOfBirth!!)
      assertThat(searchResult1.sourceSystem).isEqualTo(personEntity1.sourceSystem)
      assertThat(searchResult1.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(searchResult1.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(personEntity1))
      assertThat(searchResult1.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(personEntity1))
      assertThat(searchResult1.addresses).hasSize(personEntity1.addresses.size)

      val searchResult2 = personSearchResponse.clusters.first().results.last()
      assertThat(searchResult2.name.firstName).isEqualTo(personEntity2.getPrimaryName().firstName!!)
      assertThat(searchResult2.name.lastName).isEqualTo(personEntity2.getPrimaryName().lastName!!)
      assertThat(searchResult2.name.dateOfBirth).isEqualTo(personEntity2.getPrimaryName().dateOfBirth!!)
      assertThat(searchResult2.sourceSystem).isEqualTo(personEntity2.sourceSystem)
      assertThat(searchResult2.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(searchResult2.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(personEntity2))
      assertThat(searchResult2.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(personEntity2))
      assertThat(searchResult2.addresses).hasSize(personEntity2.addresses.size)
    }

//    @Test
//    fun `multi cluster - should return correct search result`() {
//      val prisonNumber1 = randomPrisonNumber()
//      val prisonNumber2 = randomPrisonNumber()
//      val prisonNumber3 = randomPrisonNumber()
//      val prisonNumber4 = randomPrisonNumber()
//      val cluster1 = createPersonKey()
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber2))
//      val cluster2 = createPersonKey()
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber3))
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber4))
//      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
//      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber2 }
//      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber3 }
//      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }
//
//      val personMatchScores = listOf(
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 80.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 60.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 50.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 90.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//      )
//
//      authSetup()
//      stubPostRequest(
//        url = "/person/search",
//        responseBody = jsonMapper.writeValueAsString(personMatchScores),
//      )
//
//      val search = strongestPersonFromCluster1.pseudonyms.first { it.nameType == NameType.PRIMARY }
//      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
//        url = "/person/vetting/search",
//        roles = listOf(API_VETTING_SEARCH_ONLY),
//        expectedStatus = HttpStatus.OK,
//        body = VettingPersonSearchRequest(
//          firstName = search.firstName!!,
//          lastName = search.lastName!!,
//          dateOfBirth = search.dateOfBirth!!,
//        ),
//      ).returnResult().responseBody!!
//      assertThat(personSearchResponse.data).hasSize(2)
//      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(strongestPersonFromCluster1.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.first().linkedRecords).hasSize(1)
//
//      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(strongestPersonFromCluster2.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.last().linkedRecords).hasSize(1)
//    }
//
//    @Test
//    fun `should return correct search result removing any court persons present`() {
//      val prisonNumber1 = randomPrisonNumber()
//      val courtId1 = randomCId()
//      val defendantId2 = randomDefendantId()
//      val prisonNumber4 = randomPrisonNumber()
//      val cluster1 = createPersonKey()
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
//        .addPerson(createRandomLibraPersonDetails(courtId1))
//      val cluster2 = createPersonKey()
//        .addPerson(createRandomCommonPlatformPersonDetails(defendantId2))
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber4))
//      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
//      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.cId == courtId1 }
//      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.defendantId == defendantId2 }
//      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }
//
//      val personMatchScores = listOf(
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 80.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 60.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 50.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 90.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//      )
//
//      authSetup()
//      stubPostRequest(
//        url = "/person/search",
//        responseBody = jsonMapper.writeValueAsString(personMatchScores),
//      )
//
//      val searchNamesUsingCommonPlatformDetails = strongestPersonFromCluster2.getPrimaryName()
//      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
//        url = "/person/vetting/search",
//        roles = listOf(API_VETTING_SEARCH_ONLY),
//        expectedStatus = HttpStatus.OK,
//        body = VettingPersonSearchRequest(
//          firstName = searchNamesUsingCommonPlatformDetails.firstName!!,
//          lastName = searchNamesUsingCommonPlatformDetails.lastName!!,
//          dateOfBirth = searchNamesUsingCommonPlatformDetails.dateOfBirth!!,
//        ),
//      ).returnResult().responseBody!!
//      assertThat(personSearchResponse.data).hasSize(2)
//      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(strongestPersonFromCluster1.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.first().linkedRecords).isEmpty()
//
//      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(weakestPersonFromCluster2.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.last().linkedRecords).isEmpty()
//    }
//
//    @Test
//    fun `court record matches but prison record in same cluster does not - still returns prison record`() {
//      val prisonNumber1 = randomPrisonNumber()
//      val cId1 = randomCId()
//      val defendantId2 = randomDefendantId()
//      val prisonNumber4 = randomPrisonNumber()
//      val cluster1 = createPersonKey()
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
//        .addPerson(createRandomLibraPersonDetails(cId1))
//      val cluster2 = createPersonKey()
//        .addPerson(createRandomCommonPlatformPersonDetails(defendantId2))
//        .addPerson(createRandomPrisonPersonDetails(prisonNumber4))
//      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.cId == cId1 }
//      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
//      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.defendantId == defendantId2 }
//      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }
//
//      // does not return the prison record in cluster 1 ("weakestPersonFromCluster1")
//      val personMatchScores = listOf(
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 80.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster2.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 60.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//        PersonMatchScore(
//          candidateMatchId = strongestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 90.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//      )
//
//      authSetup()
//      stubPostRequest(
//        url = "/person/search",
//        nextScenarioState = "call to get match score for prison person",
//        responseBody = jsonMapper.writeValueAsString(personMatchScores),
//      )
//
//      val prisonPersonMatchScore = listOf(
//        PersonMatchScore(
//          candidateMatchId = weakestPersonFromCluster1.matchId.toString(),
//          candidateMatchProbability = 0.9999F,
//          candidateMatchWeight = 50.0000F,
//          candidateShouldJoin = true,
//          candidateShouldFracture = false,
//        ),
//      )
//      stubPostRequest(
//        url = "/person/search",
//        currentScenarioState = "call to get match score for prison person",
//        responseBody = jsonMapper.writeValueAsString(prisonPersonMatchScore),
//      )
//
//      val searchNamesUsingCommonPlatformDetails = strongestPersonFromCluster2.getPrimaryName()
//      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
//        url = "/person/vetting/search",
//        roles = listOf(API_VETTING_SEARCH_ONLY),
//        expectedStatus = HttpStatus.OK,
//        body = VettingPersonSearchRequest(
//          firstName = searchNamesUsingCommonPlatformDetails.firstName!!,
//          lastName = searchNamesUsingCommonPlatformDetails.lastName!!,
//          dateOfBirth = searchNamesUsingCommonPlatformDetails.dateOfBirth!!,
//        ),
//      ).returnResult().responseBody!!
//      assertThat(personSearchResponse.data).hasSize(2)
//      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(weakestPersonFromCluster2.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.first().linkedRecords).isEmpty()
//
//      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(weakestPersonFromCluster1.getPrimaryName().firstName)
//      assertThat(personSearchResponse.data.last().linkedRecords).isEmpty()
//    }
//
//    @Test
//    fun `no matches found - should return empty list`() {
//      authSetup()
//      stubPostRequest(
//        url = "/person/search",
//        responseBody = jsonMapper.writeValueAsString(emptyList<PersonMatchScore>()),
//      )
//
//      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
//        url = "/person/vetting/search",
//        roles = listOf(API_VETTING_SEARCH_ONLY),
//        expectedStatus = HttpStatus.OK,
//        body = VettingPersonSearchRequest(
//          firstName = randomLowerCaseString(),
//          lastName = randomLowerCaseString(),
//          dateOfBirth = randomDate(),
//        ),
//      ).returnResult().responseBody!!
//
//      assertThat(personSearchResponse.data).isEmpty()
//    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = "/person/vetting/search",
        body = VettingPersonSearchRequest(
          firstName = randomLowerCaseString(),
          lastName = randomLowerCaseString(),
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
        url = "/person/vetting/search",
        body = VettingPersonSearchRequest(
          firstName = randomLowerCaseString(),
          lastName = randomLowerCaseString(),
          dateOfBirth = randomDate(),
        ),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }
}
