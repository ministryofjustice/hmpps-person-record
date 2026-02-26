package uk.gov.justice.digital.hmpps.personrecord.api.controller.person

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus.MATCH
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus.NO_MATCH
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus.POSSIBLE_MATCH
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class PersonCommonPlatformE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return match status when common platform record matches a probation record`() {
      val defendantId = randomDefendantId()

      val probationPerson = Person.from(createRandomProbationCase())
      val commonPlatformPerson = probationPerson.copy(crn = null, sourceSystem = COMMON_PLATFORM, defendantId = defendantId)

      val matchingProbationPerson = createPerson(probationPerson)
      val matchingCommonPlatformPerson = createPerson(commonPlatformPerson)

      createPersonKey()
        .addPerson(matchingProbationPerson)
        .addPerson(matchingCommonPlatformPerson)

      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(MATCH.name)
    }

    @Test
    fun `should return possible match status when common platform record possibly matches a probation record`() {
      val defendantId = randomDefendantId()

      val probationPerson = Person.from(createRandomProbationCase())
      val commonPlatformPersonMatchWeightApproxNine = probationPerson.copy(crn = null, sourceSystem = COMMON_PLATFORM, defendantId = defendantId, addresses = listOf(), firstName = randomName(), lastName = randomName(), middleNames = randomName(), sentences = listOf(), aliases = listOf())

      val possiblyMatchingProbationPerson = createPerson(probationPerson)
      val possiblyMatchingCommonPlatformPerson = createPerson(commonPlatformPersonMatchWeightApproxNine)

      createPersonKey()
        .addPerson(possiblyMatchingProbationPerson)

      createPersonKey()
        .addPerson(possiblyMatchingCommonPlatformPerson)

      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(POSSIBLE_MATCH.name)
    }

    @Test
    fun `should return no match status when common platform record does not match a probation record`() {
      val probationPerson = Person.from(createRandomProbationCase())
      val nonMatchingCommonPlatformPerson = createRandomCommonPlatformPersonDetails()

      createPersonKey()
        .addPerson(createPerson(nonMatchingCommonPlatformPerson))

      createPersonKey()
        .addPerson(probationPerson)

      webTestClient.get()
        .uri(matchDetailsUrl(nonMatchingCommonPlatformPerson.defendantId.toString()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(NO_MATCH.name)
    }

    @Test
    fun `should return no match status when 2 common platform records are matched but no probation record`() {
      val commonPlatformData = createRandomCommonPlatformPersonDetails()

      val commonPlatformPersonOne = createPerson(commonPlatformData)
      val commonPlatformPersonTwo = createPerson(commonPlatformData.copy(defendantId = randomDefendantId()))

      createPersonKey()
        .addPerson(commonPlatformPersonOne)
        .addPerson(commonPlatformPersonTwo)

      webTestClient.get()
        .uri(matchDetailsUrl(commonPlatformData.defendantId.toString()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(NO_MATCH.name)
    }

    private fun matchDetailsUrl(defendantId: String) = "/person/commonplatform/$defendantId/match-details"
  }
}
