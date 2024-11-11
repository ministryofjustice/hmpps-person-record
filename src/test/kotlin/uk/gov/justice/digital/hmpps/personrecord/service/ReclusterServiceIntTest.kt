package uk.gov.justice.digital.hmpps.personrecord.service

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.person.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class ReclusterServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Test
  fun `should log event if cluster needs attention`() {
    val personKeyEntity = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )

    val cluster = await untilNotNull { personKeyRepository.findByPersonId(personKeyEntity.personId) }
    reclusterService.recluster(cluster)

    checkTelemetry(
      TelemetryEventType.CPR_UUID_RECLUSTER_NEEDS_ATTENTION,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
  }

  @Test
  fun `should verify multiple records in cluster match to each other`() {
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.9999991,
      ),
    )
    stubMatchScore(matchResponse)

    val cluster = await untilNotNull { personKeyRepository.findByPersonId(personKeyEntity.personId) }
    reclusterService.recluster(cluster)
  }

  @Test
  fun `should verify multiple records in cluster do not match to each other`() {
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.600000,
      ),
    )
    stubMatchScore(matchResponse)

    val cluster = await untilNotNull { personKeyRepository.findByPersonId(personKeyEntity.personId) }
    reclusterService.recluster(cluster)
  }
}
