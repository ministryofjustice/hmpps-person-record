package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.NewReclusterService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

@ActiveProfiles("e2e")
class ReclusterServiceE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var reclusterService: NewReclusterService

  @Nested
  inner class ClusterWithExclusionOverride {

    @Test
    fun `should merge to an excluded cluster that has exclusion to the updated cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(from = basePersonData, crn = randomCrn()))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(from = basePersonData, crn = randomCrn()))
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(from = basePersonData, crn = randomCrn()))
      val cluster3 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)
      excludeRecord(personB, personC)

      recluster(personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(ACTIVE)
    }
  }

  private fun recluster(personA: PersonEntity) {
    personRepository.findByMatchId(personA.matchId)?.let {
      reclusterService.recluster(it)
    }
  }
}
