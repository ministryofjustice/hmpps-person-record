package uk.gov.justice.digital.hmpps.personrecord.api.controller.person

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class PersonCommonPlatformControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return no match status when no records match`() {
      stubPersonMatchScores()

      val crn1 = randomCrn()
      val defendantId = randomDefendantId()

      createPersonKey()
        .addPerson(createRandomCommonPlatformPersonDetails(defendantId))

      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(MatchStatus.NO_MATCH.name)
    }
  }

  @Test
  fun `should return match status when records match `() {
    stubPersonMatchScores()
    val crn = randomCrn()
    val defendantId = randomDefendantId()

    val personOne = createRandomCommonPlatformPersonDetails(defendantId = defendantId)
    val personTwo = createRandomProbationPersonDetails(crn).copy(
      titleCode = personOne.titleCode,
      firstName = personOne.firstName,
      middleNames = personOne.middleNames,
      lastName = personOne.lastName,
      dateOfBirth = personOne.dateOfBirth,
      addresses = personOne.addresses,
      aliases = personOne.aliases,
      references = personOne.references,
      sourceSystem = SourceSystemType.DELIUS,
      defendantId = null,
      masterDefendantId = personOne.masterDefendantId,
    )

    createPersonKey()
      .addPerson(personOne)
      .addPerson(personTwo)

    webTestClient.get()
      .uri(matchDetailsUrl(defendantId))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.matchStatus").isEqualTo(MatchStatus.MATCH.name)
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return 404 when defendantId not found`() {
      webTestClient.get()
        .uri(matchDetailsUrl(randomDefendantId()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when no auth header`() {
      webTestClient.get()
        .uri(matchDetailsUrl("unauthorised"))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return FORBIDDEN 403 when role is wrong`() {
      val defendantId = randomDefendantId()
      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  private fun matchDetailsUrl(defendantId: String) = "/person/commonplatform/$defendantId/match-details"
}
