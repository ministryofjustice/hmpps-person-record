package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
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

      val request = listOf(toBeMerged.personKey!!.personUUID!!.toString())

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
      val secondCrn = randomCrn()
      val cluster = createPersonKey().addPerson(
        createRandomProbationPersonDetails(crn = firstCrn),
      )
        .addPerson(createRandomProbationPersonDetails(crn = secondCrn))
      val request = listOf(cluster.personUUID!!.toString())

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
      val first = personRepository.findByCrn(firstCrn)!!
      val second = personRepository.findByCrn(secondCrn)!!
      val recordToCheck = when {
        first.personKey!!.personUUID != cluster.personUUID -> first else -> second
      }
      checkEventLog(recordToCheck.crn!!, CPRLogEvents.CPR_UUID_CREATED) { eventLogs ->
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isNotEqualTo(cluster.personUUID)
      }

      checkEventLogExist(secondCrn, CPRLogEvents.CPR_UUID_SPLIT)
    }

    @Test
    fun `should split into three clusters when there are triplets`() {
      val firstTripletCrn = randomCrn()
      val secondTripletCrn = randomCrn()
      val thirdTripletCrn = randomCrn()
      val cluster = createPersonKey()
        .addPerson(
          createRandomProbationPersonDetails(crn = firstTripletCrn),
        ).addPerson(
          createRandomProbationPersonDetails(crn = secondTripletCrn),
        )
        .addPerson(createRandomProbationPersonDetails(crn = thirdTripletCrn))
      val request = listOf(cluster.personUUID!!.toString())

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_TWINS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        val first = personRepository.findByCrn(firstTripletCrn)!!
        val second = personRepository.findByCrn(secondTripletCrn)!!
        val third = personRepository.findByCrn(thirdTripletCrn)!!
        assertThat(first.personKey!!.personUUID).isNotEqualTo(second.personKey!!.personUUID)
        assertThat(second.personKey!!.personUUID).isNotEqualTo(third.personKey!!.personUUID)
        assertThat(first.personKey!!.personUUID).isNotEqualTo(third.personKey!!.personUUID)
      }
    }

    @Test
    fun `should split into two clusters of 2 records each when there are twins with one record matching each`() {
      val firstCrn = randomCrn()
      val firstPair = createRandomProbationPersonDetails(crn = firstCrn)
      val secondCrn = randomCrn()
      val thirdCrn = randomCrn()
      val secondPair = createRandomProbationPersonDetails(crn = thirdCrn)
      val fourthCrn = randomCrn()
      val cluster = createPersonKey().addPerson(
        firstPair,
      )
        .addPerson(firstPair.copy(crn = secondCrn))
        .addPerson(secondPair)
        .addPerson(secondPair.copy(crn = fourthCrn))

      val request = listOf(cluster.personUUID!!.toString())

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
        val third = personRepository.findByCrn(thirdCrn)!!
        val fourth = personRepository.findByCrn(fourthCrn)!!
        assertThat(first.personKey!!.personUUID).isEqualTo(second.personKey!!.personUUID)
        assertThat(second.personKey!!.personUUID).isNotEqualTo(third.personKey!!.personUUID)
        assertThat(third.personKey!!.personUUID).isEqualTo(fourth.personKey!!.personUUID)
      }
      val first = personRepository.findByCrn(firstCrn)!!
      val second = personRepository.findByCrn(secondCrn)!!
      val third = personRepository.findByCrn(thirdCrn)!!
      val fourth = personRepository.findByCrn(fourthCrn)!!

      val eventLogClustersToCheck = listOf(first, second, third, fourth).filter { it.personKey!!.personUUID != cluster.personUUID }
      val eventLogClustersToCheckWithoutUUIDCreated = listOf(first, second, third, fourth).filter { it.personKey!!.personUUID == cluster.personUUID }

      val eventLogs = eventLogClustersToCheck.map { recordToCheck ->
        eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(
          CPRLogEvents.CPR_UUID_CREATED,
          recordToCheck.crn!!,
        )
      }.filter { it?.size == 1 }
      assertThat(eventLogs.size).isEqualTo(1)

      val noEventLogs = eventLogClustersToCheckWithoutUUIDCreated.map { recordToCheck ->
        eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(
          CPRLogEvents.CPR_UUID_CREATED,
          recordToCheck.crn!!,
        )
      }.filter { it?.size == 1 }
      assertThat(noEventLogs.isEmpty()).isTrue()
    }
  }

  companion object {
    private const val ADMIN_RECLUSTER_TWINS_URL = "/admin/recluster-twins"
  }
}
