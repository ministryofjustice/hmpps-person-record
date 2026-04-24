package uk.gov.justice.digital.hmpps.personrecord.service

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED

class PersonDeletionServiceIntTest : WebTestBase() {

  @MockitoSpyBean
  private lateinit var spyReclusterService: ReclusterService

  @Nested
  inner class SinglePersonCluster {
    @Test
    fun `deletes person and cluster - correct events occurred`() {
      val personToBeDeleted = createPerson(createRandomPrisonPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personToBeDeleted)
        .also { stubDeletePersonMatch() }

      triggerPersonDeletion(personToBeDeleted.prisonNumber)

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(personToBeDeleted.prisonNumber!!)).isNull()
        assertThat(personKeyRepository.findByPersonUUID(cluster.personUUID)).isNull()

        checkTelemetry(CPR_RECORD_DELETED, mapOf("UUID" to cluster.personUUID.toString()))
        checkTelemetry(CPR_UUID_DELETED, mapOf("UUID" to cluster.personUUID.toString()))
        checkEventLogExist(personToBeDeleted.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkEventLogExist(personToBeDeleted.prisonNumber!!, CPRLogEvents.CPR_UUID_DELETED)
      }

      verifyNoInteractions(spyReclusterService)
      verifyDeleteFromPersonMatch(personToBeDeleted)
    }
  }

  @Nested
  inner class MultiplePersonCluster {
    @Test
    fun `deletes persons but not cluster - correct events occurred`() {
      val personToBeDeleted = createPerson(createRandomPrisonPersonDetails())
      val personToRemain = createPerson(createRandomPrisonPersonDetails())

      val cluster = createPersonKey()
        .addPerson(personToBeDeleted)
        .addPerson(personToRemain)
        .also {
          stubDeletePersonMatch()
          stubPersonMatchScores()
        }

      triggerPersonDeletion(personToBeDeleted.prisonNumber)

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(personToBeDeleted.prisonNumber!!)).isNull()
        val clusterAfterDelete = personKeyRepository.findByPersonUUID(cluster.personUUID)!!
        assertThat(clusterAfterDelete.personEntities.size).isEqualTo(1)

        checkTelemetry(CPR_RECORD_DELETED, mapOf("UUID" to cluster.personUUID.toString()))
        checkEventLogExist(personToBeDeleted.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
      }

      verify(spyReclusterService, times(1)).recluster(any())
      verifyDeleteFromPersonMatch(personToBeDeleted)
    }
  }

  @Nested
  inner class PersonMergeScenarios {
    @Test
    fun `deletes a merged from person - correct events occurred`() {
      val toPerson = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val fromPerson = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }
      stubDeletePersonMatch()

      triggerPersonDeletion(fromPerson.prisonNumber)

      awaitAssert {
        awaitNotNull { personKeyRepository.findByPersonUUID(toPerson.personKey!!.personUUID) }
        awaitNotNull { personRepository.findByPrisonNumber(toPerson.prisonNumber!!) }
        assertThat(personRepository.findByPrisonNumber(fromPerson.prisonNumber!!)).isNull()

        checkEventLogExist(fromPerson.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkTelemetry(CPR_RECORD_DELETED, mapOf("PRISON_NUMBER" to fromPerson.prisonNumber))
      }

      verifyNoInteractions(spyReclusterService)
      verifyDeleteFromPersonMatch(fromPerson)
    }

    @Test
    fun `deletes a non merged person - correct events occurred`() {
      val toPerson = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val fromPersonC = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }
      val fromPersonB = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }
      val fromPersonA = createPerson(createRandomPrisonPersonDetails()) { mergedTo = fromPersonB.id }
      stubDeletePersonMatch()

      triggerPersonDeletion(toPerson.prisonNumber)

      awaitAssert {
        assertThat(personKeyRepository.findByPersonUUID(toPerson.personKey!!.personUUID)).isNull()
        assertThat(personRepository.findByPrisonNumber(fromPersonA.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(fromPersonB.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(fromPersonC.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(toPerson.prisonNumber!!)).isNull()

        checkEventLogExist(toPerson.prisonNumber!!, CPRLogEvents.CPR_UUID_DELETED)
        checkEventLogExist(toPerson.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkEventLogExist(fromPersonA.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkEventLogExist(fromPersonB.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkEventLogExist(fromPersonC.prisonNumber!!, CPRLogEvents.CPR_RECORD_DELETED)
        checkTelemetry(CPR_UUID_DELETED, mapOf("UUID" to toPerson.personKey!!.personUUID.toString()))
        checkTelemetry(CPR_RECORD_DELETED, mapOf("UUID" to toPerson.personKey!!.personUUID.toString()))
        checkTelemetry(CPR_RECORD_DELETED, mapOf("PRISON_NUMBER" to fromPersonA.prisonNumber))
        checkTelemetry(CPR_RECORD_DELETED, mapOf("PRISON_NUMBER" to fromPersonB.prisonNumber))
        checkTelemetry(CPR_RECORD_DELETED, mapOf("PRISON_NUMBER" to fromPersonC.prisonNumber))
      }

      verifyNoInteractions(spyReclusterService)
      verifyDeleteFromPersonMatch(toPerson)
    }
  }

  @Nested
  inner class OverrideMarkerScenarios {
    @Test
    fun `deletes person with override marker equal to null - does not send event`() {
      val personToBeDeleted = createPerson(createRandomPrisonPersonDetails()) { overrideMarker = null }
      val cluster = createPersonKey()
        .addPerson(personToBeDeleted)
        .also { stubDeletePersonMatch() }

      triggerPersonDeletion(personToBeDeleted.prisonNumber)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf(
          "UUID" to cluster.personUUID.toString(),
          "UUID_OF_OVERRIDE_CLUSTER" to "null",
        ),
      )
    }

    @Test
    fun `deletes person with override marker present - sends event`() {
      val otherCluster = createPersonKey()
        .addPerson(createPerson(createRandomPrisonPersonDetails()) { overrideMarker = null })

      val personToBeDeleted = createPerson(createRandomPrisonPersonDetails()) { overrideMarker = otherCluster.personUUID }
      val clusterWithPersonWithOverrideMarker = createPersonKey()
        .addPerson(personToBeDeleted)

      stubDeletePersonMatch()

      triggerPersonDeletion(personToBeDeleted.prisonNumber)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf(
          "UUID" to clusterWithPersonWithOverrideMarker.personUUID.toString(),
          "UUID_OF_OVERRIDE_CLUSTER" to otherCluster.personUUID.toString(),
        ),
      )
    }
  }

  private fun triggerPersonDeletion(prisonNumber: String?) = sendDeleteRequestAsserted<Unit>(
    url = "/person/prison/$prisonNumber",
    roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
    expectedStatus = HttpStatus.OK,
  )

  private fun verifyDeleteFromPersonMatch(personEntity: PersonEntity) {
    wiremock.verify(
      1,
      deleteRequestedFor(urlEqualTo("/person"))
        .withRequestBody(equalToJson("""{"matchId":"${personEntity.matchId}"}""")),
    )
  }
}
