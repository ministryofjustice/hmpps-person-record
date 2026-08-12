package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonReligionMergeHandlerIntTest.Companion.prisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

class PrisonMergeEventListenerIntTest : PrisonEventListenerTestBase() {

  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  @Autowired
  lateinit var prisonReligionRepository: PrisonReligionRepository

  @Autowired
  lateinit var prisonMergeEventProcessor: PrisonMergeEventProcessor

  @Nested
  inner class MissingFromRecord {

    @Test
    fun `processes prisoner merge event when source record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      prisonMergeEventAndResponseSetup(sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("PRISON_NUMBER" to targetPrisonNumber))
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class MissingToRecord {

    @Test
    fun `processes prisoner merge event when target record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      val targetPerson = awaitNotNull { personRepository.findByPrisonNumber(targetPrisonNumber) }

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_CREATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }
  }

  // TODO this is a proof of concept and would need to be rationalised when taking this code forward.
  @Nested
  inner class MergingReligion {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    fun prisonResponseSetupForMergeEvent(
      targetPrisonNumber: String,
      scenario: String = BASE_SCENARIO,
      currentScenarioState: String = STARTED,
      nextScenarioState: String = STARTED,
    ) {
      stubPrisonResponse(
        ApiResponseSetup(prisonNumber = targetPrisonNumber),
        scenario,
        currentScenarioState,
        nextScenarioState,
      )
    }

    @Test
    fun `religion history is correctly merged`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      val fromPerson = createPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = NOMIS))
      val toPerson = createPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = NOMIS))

      prisonReligionRepository.saveAll(
        listOf(
          prisonReligionEntity {
            it.prisonRecordType = CURRENT
            it.prisonNumber = fromPerson.prisonNumber!!
            it.startDate = LocalDate.of(2021, 1, 25)
            it.code = AGNO
          },
          prisonReligionEntity {
            it.prisonRecordType = CURRENT
            it.prisonNumber = toPerson.prisonNumber!!
            it.startDate = LocalDate.of(2021, 1, 1)
            it.code = CALV
          },
        ),
      )

      prisonResponseSetupForMergeEvent(targetPrisonNumber = toPrisonNumber)

      // Call the processor directly as we are not testing raising events, and it causes async issues if we do.
      prisonMergeEventProcessor.processEvent(
        PrisonPersonMerged(
          personReference = PersonReference(listOf(PersonIdentifier("NOMS", toPrisonNumber))),
          additionalInformation = PrisonPersonMergedInfo(sourcePrisonNumber = fromPrisonNumber),
        ),
      )

      assertThat(personRepository.findByPrisonNumber(toPerson.prisonNumber!!)?.religion).isEqualTo(AGNO)
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber = sourcePrisonNumber,
        targetPrisonNumber = targetPrisonNumber,
      )

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val sourceCluster = createPersonKey()
        .addPerson(Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS))
        .addPerson(sourcePerson)
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      sourceCluster.assertClusterIsOfSize(1)
      sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source doesn't have an UUID`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      val sourceClusterId = sourcePerson.personKey?.id

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      val sourceClusterPostMerge = personKeyRepository.findById(sourceClusterId!!).getOrNull()
      assertThat(sourceClusterPostMerge).isNull()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
        assertThat(eventLogs.first().personUUID).isEqualTo(sourcePerson.personKey!!.personUUID)
      }
    }

    @Test
    fun `should retry on 500 error`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      stub5xxResponse(prisonURL(targetPrisonNumber), "next request will succeed", "retry")

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber,
        targetPrisonNumber,
        scenario = "retry",
        currentScenarioState = "next request will succeed",
      )

      expectNoMessagesOnQueueOrDlq(prisonMergeEventsQueue)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should put on dlq when message processing fails`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")
      stub5xxResponse(
        prisonURL(targetPrisonNumber),
        "PrisonMergeEventProcessingWillFail",
        "failure",
        "PrisonMergeEventProcessingWillFail",
      )
      stub5xxResponse(
        prisonURL(targetPrisonNumber),
        "PrisonMergeEventProcessingWillFail",
        "failure",
        "PrisonMergeEventProcessingWillFail",
      )

      publishPrisonPersonMergedEvent(targetPrisonNumber, sourcePrisonNumber)

      expectOneMessageOnDlq(prisonMergeEventsQueue)
    }
  }

  @Nested
  inner class PassiveStateRecords {

    @BeforeEach
    fun beforeEach() {
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event and target record passive state is maintained`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson =
        createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber = sourcePrisonNumber,
        targetPrisonNumber = targetPrisonNumber,
      )

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isFalse()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isTrue()
      wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event and source record passive state is not maintained on target record`() {
      stubPersonMatchUpsert()

      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson =
        createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber = sourcePrisonNumber,
        targetPrisonNumber = targetPrisonNumber,
      )

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isTrue()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isFalse()
      wiremock.verify(1, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }
  }
}
