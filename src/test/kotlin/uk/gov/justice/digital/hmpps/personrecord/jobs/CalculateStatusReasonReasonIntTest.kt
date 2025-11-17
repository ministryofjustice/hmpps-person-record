package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import java.util.UUID

class CalculateStatusReasonReasonIntTest : WebTestBase() {

  @Test
  fun `should recalculate status reason code for broken cluster`() {
    val cluster = personKeyRepository.save(
      PersonKeyEntity(
        personUUID = UUID.randomUUID(),
        status = UUIDStatusType.NEEDS_ATTENTION,
        statusReason = null,
      ),
    )
      .addPerson(createPerson(createRandomProbationPersonDetails()))
      .addPerson(createPerson(createRandomProbationPersonDetails()))

    stubPersonMatchScores()

    webTestClient.post()
      .uri("/admin/calculate-status-reason")
      .exchange()
      .expectStatus()
      .isOk

    cluster.assertClusterIsOfSize(2)
    cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION, UUIDStatusReasonType.BROKEN_CLUSTER)
  }

  @Test
  fun `should not raise broken cluster if can self heal`() {
    val person = createPerson(createRandomProbationPersonDetails())
    val matchesPerson = createPerson(createRandomProbationPersonDetails())
    val cluster = personKeyRepository.save(
      PersonKeyEntity(
        personUUID = UUID.randomUUID(),
        status = UUIDStatusType.NEEDS_ATTENTION,
        statusReason = null,
      ),
    )
      .addPerson(person)
      .addPerson(matchesPerson)

    stubOnePersonMatchAboveJoinThreshold(matchId = person.matchId, matchedRecord = matchesPerson.matchId)

    webTestClient.post()
      .uri("/admin/calculate-status-reason")
      .exchange()
      .expectStatus()
      .isOk

    cluster.assertClusterIsOfSize(2)
    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
  }
}
