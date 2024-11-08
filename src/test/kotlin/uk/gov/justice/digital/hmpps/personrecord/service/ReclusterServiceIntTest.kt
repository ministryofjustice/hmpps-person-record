package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
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
    val crn = randomCRN()
    val personKey = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
    val personEntity = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKey,
    )

    reclusterService.recluster(personEntity)

    checkTelemetry(
      TelemetryEventType.CPR_UUID_RECLUSTER_NEEDS_ATTENTION,
      mapOf(
        "CRN" to crn,
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
        "UUID" to personKey.personId.toString(),
      ),
    )
  }
}
