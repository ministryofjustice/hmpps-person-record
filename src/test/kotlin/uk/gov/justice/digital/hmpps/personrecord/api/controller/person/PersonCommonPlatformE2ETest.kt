package uk.gov.justice.digital.hmpps.personrecord.api.controller.person

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class PersonCommonPlatformE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return match status when record matches`() {
      val basePersonDataProbation = createRandomProbationCase()

      val defendantId = randomDefendantId()

      val probationPerson = Person.from(basePersonDataProbation)
      val commonPlatformPerson = probationPerson.copy(crn = null, sourceSystem = SourceSystemType.COMMON_PLATFORM, defendantId = defendantId)

      val createProbationPerson = createPerson(probationPerson)
      val createCommonPlatformPerson = createPerson(commonPlatformPerson)

      createPersonKey()
        .addPerson(createProbationPerson)
        .addPerson(createCommonPlatformPerson)

      webTestClient.get()
        .uri(matchDetailsUrl(defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(MatchStatus.MATCH.name)
    }

    @Test
    fun `should return no match status when record do not match`() {
      val basePersonDataProbation = createRandomProbationCase()

      val probationPerson = Person.from(basePersonDataProbation)
      val commonPlatformPerson = createRandomCommonPlatformPersonDetails()

      val createCommonPlatformPerson = createPerson(commonPlatformPerson)

      createPersonKey()
        .addPerson(createCommonPlatformPerson)

      createPersonKey()
        .addPerson(probationPerson)

      webTestClient.get()
        .uri(matchDetailsUrl(commonPlatformPerson.defendantId.toString()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(MatchStatus.NO_MATCH.name)
    }

    @Test
    fun `should return no match status when 2 common record are not matched`() {

      val commonPlatformPersonOne = createRandomCommonPlatformPersonDetails()

      val createCommonPlatformPersonOne = createPerson(commonPlatformPersonOne)
      val createCommonPlatformPersonTwo = createPerson(commonPlatformPersonOne.copy(defendantId = randomDefendantId()))

      createPersonKey()
        .addPerson(createCommonPlatformPersonOne)
        .addPerson(createCommonPlatformPersonTwo)


      webTestClient.get()
        .uri(matchDetailsUrl(commonPlatformPersonOne.defendantId.toString()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.matchStatus").isEqualTo(MatchStatus.NO_MATCH.name)
    }
    private fun matchDetailsUrl(defendantId: String) = "/person/commonplatform/$defendantId/match-details"
  }
}
