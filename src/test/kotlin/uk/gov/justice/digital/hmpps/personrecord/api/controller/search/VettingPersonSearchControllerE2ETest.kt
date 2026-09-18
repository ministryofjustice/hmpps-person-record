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
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class VettingPersonSearchControllerE2ETest : E2ETestBase() {

  @Nested
  inner class Success {

    @Test
    fun `single cluster - should return correct search result`() {
      val prisonNumber = randomPrisonNumber()
      val crn = randomCrn()
      val basePerson = createRandomPrisonPersonDetails()
      val cluster = createPersonKey()
        .addPerson(createPerson(basePerson.copy(prisonNumber = prisonNumber)))
        .addPerson(createPerson(basePerson.copy(crn = crn, sourceSystem = SourceSystemType.DELIUS)))
      val prisonPersonEntity = cluster.personEntities.first { it.prisonNumber == prisonNumber }
      val probationPersonEntity = cluster.personEntities.first { it.crn == crn }

      val vettingSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = prisonPersonEntity.getPrimaryName().firstName!!,
          lastName = prisonPersonEntity.getPrimaryName().lastName!!,
          dateOfBirth = prisonPersonEntity.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(vettingSearchResponse.data).hasSize(1) // results span only 1 cluster
      assertThat(vettingSearchResponse.data.first().results).hasSize(2) // the cluster has 2 records on it

      val searchResult1 = vettingSearchResponse.data.first().results.first()
      assertThat(searchResult1.name.firstName).isEqualTo(prisonPersonEntity.getPrimaryName().firstName!!)
      assertThat(searchResult1.name.lastName).isEqualTo(prisonPersonEntity.getPrimaryName().lastName!!)
      assertThat(searchResult1.name.dateOfBirth).isEqualTo(prisonPersonEntity.getPrimaryName().dateOfBirth!!)
      assertThat(searchResult1.sourceSystem).isEqualTo(prisonPersonEntity.sourceSystem)
      assertThat(searchResult1.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(searchResult1.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(prisonPersonEntity))
      assertThat(searchResult1.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(prisonPersonEntity))
      assertThat(searchResult1.addresses).hasSize(prisonPersonEntity.addresses.size)

      val searchResult2 = vettingSearchResponse.data.first().results.last()
      assertThat(searchResult2.name.firstName).isEqualTo(probationPersonEntity.getPrimaryName().firstName!!)
      assertThat(searchResult2.name.lastName).isEqualTo(probationPersonEntity.getPrimaryName().lastName!!)
      assertThat(searchResult2.name.dateOfBirth).isEqualTo(probationPersonEntity.getPrimaryName().dateOfBirth!!)
      assertThat(searchResult2.sourceSystem).isEqualTo(probationPersonEntity.sourceSystem)
      assertThat(searchResult2.status).isEqualTo(SearchStatus.TRUSTED)
      assertThat(searchResult2.aliases).usingRecursiveComparison().isEqualTo(CanonicalAlias.from(probationPersonEntity))
      assertThat(searchResult2.identifiers).usingRecursiveComparison().isEqualTo(CanonicalSearchIdentifiers.from(probationPersonEntity))
      assertThat(searchResult2.addresses).hasSize(probationPersonEntity.addresses.size)
    }

    @Test
    fun `multi cluster - should return correct search result`() {
      val prisonNumber1 = randomPrisonNumber()
      val crn1 = randomCrn()
      val prisonNumber2 = randomPrisonNumber()
      val prisonNumber3 = randomPrisonNumber()
      val crn2 = randomCrn()

      val basePerson1 = createRandomPrisonPersonDetails()
      val cluster1 = createPersonKey()
        .addPerson(createPerson(basePerson1.copy(prisonNumber = prisonNumber1)))
        .addPerson(createPerson(basePerson1.copy(crn = crn1, prisonNumber = null, sourceSystem = SourceSystemType.DELIUS)))

      createPersonKey()
        .addPerson(createPerson(basePerson1.copy(prisonNumber = prisonNumber2)))
        .addPerson(createPerson(basePerson1.copy(prisonNumber = prisonNumber3)))
        .addPerson(createPerson(basePerson1.copy(crn = crn2, prisonNumber = null, sourceSystem = SourceSystemType.DELIUS)))
      val personEntity2 = cluster1.personEntities.first { it.crn == crn1 }

      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = personEntity2.getPrimaryName().firstName!!,
          lastName = personEntity2.getPrimaryName().lastName!!,
          dateOfBirth = personEntity2.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(2) // results span 2 cluster

      assertThat(personSearchResponse.data.first().results).hasSize(2) // the 1st cluster has 2 records on it
      assertThat(personSearchResponse.data.last().results).hasSize(3) // the 2nd cluster has 3 records on it
    }

    @Test
    fun `single cluster - court record only - does not return anything`() {
      val cluster = createPersonKey()
        .addPerson(createPerson(createRandomCommonPlatformPersonDetails()))
      val courtPersonEntity = cluster.personEntities.first()

      val vettingSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = courtPersonEntity.getPrimaryName().firstName!!,
          lastName = courtPersonEntity.getPrimaryName().lastName!!,
          dateOfBirth = courtPersonEntity.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(vettingSearchResponse.data).isEmpty()
    }

    @Test
    fun `should return correct search result removing any court persons present`() {
      val prisonNumber1 = randomPrisonNumber()
      val cid1 = randomCId()
      val defendantId2 = randomDefendantId()
      val prisonNumber4 = randomPrisonNumber()

      val basePerson = createRandomPrisonPersonDetails()
      createPersonKey()
        .addPerson(basePerson.copy(prisonNumber = prisonNumber1))
        .addPerson(basePerson.copy(cId = cid1, prisonNumber = null, sourceSystem = SourceSystemType.LIBRA))
      val cluster2 = createPersonKey()
        .addPerson(basePerson.copy(prisonNumber = prisonNumber4))
        .addPerson(basePerson.copy(defendantId = defendantId2, prisonNumber = null, sourceSystem = SourceSystemType.COMMON_PLATFORM))
      val courtPersonEntity = cluster2.personEntities.first { it.defendantId == defendantId2 }

      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = courtPersonEntity.getPrimaryName().firstName!!,
          lastName = courtPersonEntity.getPrimaryName().lastName!!,
          dateOfBirth = courtPersonEntity.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(2)
      assertThat(personSearchResponse.data.first().results).hasSize(1)
      assertThat(personSearchResponse.data.last().results).hasSize(1)
    }

    @Test
    fun `court record matches but prison record in same cluster does not - still returns prison record`() {
      val prisonNumber1 = randomPrisonNumber()
      val defendantId1 = randomDefendantId()
      val cluster = createPersonKey()
        .addPerson(createRandomCommonPlatformPersonDetails(defendantId1))
        .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
      val courtPersonEntity = cluster.personEntities.first { it.defendantId == defendantId1 }

      val personSearchResponse = sendPostRequestAsserted<VettingPersonSearchResponse>(
        url = "/person/vetting/search",
        roles = listOf(API_VETTING_SEARCH_ONLY),
        expectedStatus = HttpStatus.OK,
        body = VettingPersonSearchRequest(
          firstName = courtPersonEntity.getPrimaryName().firstName!!,
          lastName = courtPersonEntity.getPrimaryName().lastName!!,
          dateOfBirth = courtPersonEntity.getPrimaryName().dateOfBirth!!,
        ),
      ).returnResult().responseBody!!
      assertThat(personSearchResponse.data).hasSize(1)
      assertThat(personSearchResponse.data.first().results).hasSize(1)
      assertThat(personSearchResponse.data.first().results.first().sourceSystem).isEqualTo(SourceSystemType.NOMIS)
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
}
