package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_VETTING_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.CanonicalSearchIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class VettingPersonSearchE2ETest : E2ETestBase() {

  @Nested
  inner class Success {

    @Test
    fun `single cluster - should return correct search result`() {
      val strongMatchPrisonNumber = randomPrisonNumber()
      val weakMatchPrisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(strongMatchPrisonNumber)
      val cluster = createPersonKey()
        .addPerson(prisonPerson)
        .addPerson(prisonPerson.copy(prisonNumber = weakMatchPrisonNumber, lastName = prisonPerson.lastName!!.plus("a")))
      val strongestMatch = cluster.personEntities.first { it.prisonNumber == strongMatchPrisonNumber }
      val weakestMatch = cluster.personEntities.first { it.prisonNumber == weakMatchPrisonNumber }

      val strongestPersonPrimaryPseudonym = strongestMatch.getPrimaryName()
      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = strongestPersonPrimaryPseudonym.firstName!!,
          lastName = strongestPersonPrimaryPseudonym.lastName!!,
          dateOfBirth = strongestPersonPrimaryPseudonym.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(1)
      assertThat(personSearchResponse.data.first().linkedRecords).hasSize(1)

      val strongestPersonFromResponse = personSearchResponse.data.first()
      assertThat(strongestPersonFromResponse.name.firstName).isEqualTo(strongestPersonPrimaryPseudonym.firstName)
      assertThat(strongestPersonFromResponse.name.middleNames).isEqualTo(strongestPersonPrimaryPseudonym.middleNames)
      assertThat(strongestPersonFromResponse.name.lastName).isEqualTo(strongestPersonPrimaryPseudonym.lastName)
      assertThat(strongestPersonFromResponse.name.dateOfBirth).isEqualTo(strongestPersonPrimaryPseudonym.dateOfBirth)
      assertThat(strongestPersonFromResponse.sourceSystem).isEqualTo(strongestMatch.sourceSystem)
      assertThat(strongestPersonFromResponse.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(strongestPersonFromResponse.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(strongestMatch))
      assertThat(strongestPersonFromResponse.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(strongestMatch))
      assertThat(strongestPersonFromResponse.addresses).hasSize(strongestMatch.addresses.size)

      val weakestPersonFromResponse = personSearchResponse.data.first().linkedRecords.first()
      val weakestPersonPrimaryPseudonym = weakestMatch.pseudonyms.first { it.nameType == NameType.PRIMARY }
      assertThat(weakestPersonFromResponse.name.firstName).isEqualTo(weakestPersonPrimaryPseudonym.firstName)
      assertThat(weakestPersonFromResponse.name.middleNames).isEqualTo(weakestPersonPrimaryPseudonym.middleNames)
      assertThat(weakestPersonFromResponse.name.lastName).isEqualTo(weakestPersonPrimaryPseudonym.lastName)
      assertThat(weakestPersonFromResponse.name.dateOfBirth).isEqualTo(weakestPersonPrimaryPseudonym.dateOfBirth)
      assertThat(weakestPersonFromResponse.sourceSystem).isEqualTo(weakestMatch.sourceSystem)
      assertThat(weakestPersonFromResponse.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(weakestPersonFromResponse.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(weakestMatch))
      assertThat(weakestPersonFromResponse.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(weakestMatch))
      assertThat(weakestPersonFromResponse.addresses).hasSize(weakestMatch.addresses.size)
    }

    // fails, only one match instead of 2
    @Test
    fun `multi cluster - should return correct search result`() {
      val prisonNumber1 = randomPrisonNumber()
      val prisonNumber2 = randomPrisonNumber()
      val prisonNumber3 = randomPrisonNumber()
      val prisonNumber4 = randomPrisonNumber()

      val cluster1Person = createRandomPrisonPersonDetails(prisonNumber1)
      val cluster2Person = cluster1Person.copy(firstName = randomName())
      val cluster1 = createPersonKey()
        .addPerson(cluster1Person)
        .addPerson(cluster1Person.copy(prisonNumber = prisonNumber2, dateOfBirth = randomDate()))
      val cluster2 = createPersonKey()
        .addPerson(cluster2Person.copy(prisonNumber = prisonNumber3))
        .addPerson(cluster2Person.copy(prisonNumber = prisonNumber4, dateOfBirth = randomDate()))
      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber2 }
      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber3 }
      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }

      val search = strongestPersonFromCluster1.pseudonyms.first { it.nameType == NameType.PRIMARY }
      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = search.firstName!!,
          lastName = search.lastName!!,
          dateOfBirth = search.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(2)
      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(strongestPersonFromCluster1.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.first().linkedRecords).hasSize(1)

      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(strongestPersonFromCluster2.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.last().linkedRecords).hasSize(1)
    }

    // fails - only one result returned
    @Test
    fun `should return correct search result removing any court persons present`() {
      val prisonNumber1 = randomPrisonNumber()
      val cId1 = randomCId()
      val defendantId2 = randomDefendantId()
      val prisonNumber4 = randomPrisonNumber()
      val cluster1Person = createRandomPrisonPersonDetails(prisonNumber1)
      val cluster1 = createPersonKey()
        .addPerson(cluster1Person)
        .addPerson(cluster1Person.copy(prisonNumber = null, cId = cId1, sourceSystem = LIBRA, dateOfBirth = randomDate()))
      val cluster2Person = createRandomCommonPlatformPersonDetails(defendantId2)
      val cluster2 = createPersonKey()
        .addPerson(cluster2Person)
        .addPerson(cluster2Person.copy(prisonNumber = prisonNumber4, defendantId = null, sourceSystem = NOMIS, dateOfBirth = randomDate()))
      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.cId == cId1 }
      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.defendantId == defendantId2 }
      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }
      val searchNamesUsingCommonPlatformDetails = strongestPersonFromCluster2.getPrimaryName()
      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = searchNamesUsingCommonPlatformDetails.firstName!!,
          lastName = searchNamesUsingCommonPlatformDetails.lastName!!,
          dateOfBirth = searchNamesUsingCommonPlatformDetails.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(2)
      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(strongestPersonFromCluster1.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.first().linkedRecords).isEmpty()

      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(weakestPersonFromCluster2.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.last().linkedRecords).isEmpty()
    }

    @Test
    fun `court record matches but prison record in same cluster does not - still returns prison record`() {
      val prisonNumber1 = randomPrisonNumber()
      val cId1 = randomCId()
      val defendantId2 = randomDefendantId()
      val prisonNumber4 = randomPrisonNumber()
      val matchingDetails = createRandomPrisonPersonDetails(prisonNumber1)
      val cluster1 = createPersonKey()
        .addPerson(matchingDetails)
        .addPerson(createRandomLibraPersonDetails(cId1))
      val cluster2 = createPersonKey()
        .addPerson(matchingDetails.copy(defendantId = defendantId2, prisonNumber = null, sourceSystem = COMMON_PLATFORM))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber4))
      val strongestPersonFromCluster1 = cluster1.personEntities.first { it.cId == cId1 }
      val weakestPersonFromCluster1 = cluster1.personEntities.first { it.prisonNumber == prisonNumber1 }
      val strongestPersonFromCluster2 = cluster2.personEntities.first { it.defendantId == defendantId2 }
      val weakestPersonFromCluster2 = cluster2.personEntities.first { it.prisonNumber == prisonNumber4 }

      val searchNamesUsingCommonPlatformDetails = strongestPersonFromCluster2.getPrimaryName()
      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = searchNamesUsingCommonPlatformDetails.firstName!!,
          lastName = searchNamesUsingCommonPlatformDetails.lastName!!,
          dateOfBirth = searchNamesUsingCommonPlatformDetails.dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(2)
      assertThat(personSearchResponse.data.first().name.firstName).isEqualTo(weakestPersonFromCluster2.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.first().linkedRecords).isEmpty()

      assertThat(personSearchResponse.data.last().name.firstName).isEqualTo(weakestPersonFromCluster1.getPrimaryName().firstName)
      assertThat(personSearchResponse.data.last().linkedRecords).isEmpty()
    }

    @Test
    fun `no matches found - should return empty list`() {
      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = randomLowerCaseString(),
          lastName = randomLowerCaseString(),
          dateOfBirth = randomDate(),
        ),
      ).returnResult().responseBody!!

      assertThat(personSearchResponse.data).isEmpty()
    }
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

  // TODO find a better way of reusing these

  final inline fun <reified T : Any> sendPostRequestAsserted(
    url: String,
    body: Any,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> = sendRequestAsserted(url, body, roles, expectedStatus, sendAuthorised, HttpMethod.POST)
  final inline fun <reified T : Any> sendRequestAsserted(
    url: String,
    body: Any?,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
    methodType: HttpMethod,
  ): WebTestClient.BodySpec<T, *> {
    val requestSpec = webTestClient
      .method(methodType)
      .uri(url)
      .contentType(MediaType.APPLICATION_JSON)

    val requestSpecReady = when (methodType) {
      HttpMethod.GET, HttpMethod.DELETE -> requestSpec
      else -> requestSpec.bodyValue(body!!)
    }

    val responseSpec = when (sendAuthorised) {
      true -> requestSpecReady.authorised(roles).exchange()
      false -> requestSpecReady.exchange()
    }.expectStatus().isEqualTo(expectedStatus.value())
    return responseSpec.expectBody<T>()
  }
}
