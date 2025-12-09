package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminTwin
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class SplitTwinsApiE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService
  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should ignore merged record`() {
      val firstCrn = randomCrn()
      val twinDetails = createRandomProbationPersonDetails(crn = firstCrn)
      val secondCrn = randomCrn()
      val firstPerson = createPerson(twinDetails)
      val cluster = createPersonKey().addPerson(firstPerson)
      val toBeMerged = createPersonWithNewKey(twinDetails.copy(crn = randomCrn(), lastName = randomName()))
      reclusterService.recluster(firstPerson)
      awaitAssert {
        val updatedCluster = personKeyRepository.findByPersonUUID(toBeMerged.personKey?.personUUID)
        assertThat(updatedCluster?.mergedTo).isEqualTo(cluster.id)
      }
      cluster.addPerson(twinDetails.copy(crn = secondCrn))

      val request = listOf(AdminTwin(toBeMerged.personKey!!.personUUID!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_TWINS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        val first = personRepository.findByCrn(firstCrn)!!
        val second = personRepository.findByCrn(secondCrn)!!
        assertThat(first.personKey?.personUUID).isEqualTo(second.personKey?.personUUID)
      }
    }

    @Test
    fun `should split twins when only 2 records in the same cluster`() {
      val firstCrn = randomCrn()
      val twinDetails = createRandomProbationPersonDetails(crn = firstCrn)
      val secondCrn = randomCrn()
      val cluster = createPersonKey().addPerson(twinDetails).addPerson(twinDetails.copy(crn = secondCrn))
      val request = listOf(AdminTwin(cluster.personUUID!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_TWINS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        val first = personRepository.findByCrn(firstCrn)!!
        val second = personRepository.findByCrn(secondCrn)!!
        assertThat(first.personKey!!.personUUID).isNotEqualTo(second.personKey!!.personUUID)
      }
    }
  }


  companion object {
    private const val ADMIN_RECLUSTER_TWINS_URL = "/admin/recluster-twins"
  }
}
