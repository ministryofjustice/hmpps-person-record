package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService.Companion.UnmergeRecordType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_LINK_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import java.util.concurrent.TimeUnit.SECONDS

class ProbationUnmergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should create record when reactivated record not found and should create a UUID`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val personKeyEntity = createPersonKey()
      createPerson(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
        personKeyEntity = personKeyEntity,
      )

      val reactivated = ApiResponseSetup(crn = reactivatedCrn)
      val unmerged = ApiResponseSetup(crn = unmergedCrn)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

      checkTelemetry(
        UNMERGE_MESSAGE_RECEIVED,
        mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UNMERGE_RECORD_NOT_FOUND,
        mapOf("RECORD_TYPE" to UnmergeRecordType.REACTIVATED.name, "CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UNMERGE_LINK_NOT_FOUND,
        mapOf("REACTIVATED_CRN" to reactivatedCrn, "RECORD_TYPE" to UnmergeRecordType.REACTIVATED.name, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "REACTIVATED_CRN" to reactivatedCrn,
          "UNMERGED_CRN" to unmergedCrn,
          "UNMERGED_UUID" to personKeyEntity.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      val reactivatedPerson = awaitNotNullPerson { personRepository.findByCrn(reactivatedCrn) }
      assertThat(reactivatedPerson.personKey).isNotNull()
      assertThat(reactivatedPerson.personKey?.personUUID).isNotEqualTo(personKeyEntity.personUUID)
    }

    @Test
    fun `should retry on 500 error`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPersonKey = createPersonKey()
      val reactivatedPersonKey = createPersonKey()

      createPerson(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
        personKeyEntity = unmergedPersonKey,
      )
      createPerson(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
        personKeyEntity = reactivatedPersonKey,
      )
      stub5xxResponse(probationUrl(unmergedCrn), "next request will succeed", "retry")

      val reactivated = ApiResponseSetup(crn = reactivatedCrn)
      val unmerged = ApiResponseSetup(crn = unmergedCrn)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged, scenario = "retry", currentScenarioState = "next request will succeed", nextScenarioState = "next request will succeed")

      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)

      checkTelemetry(
        UNMERGE_MESSAGE_RECEIVED,
        mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "REACTIVATED_CRN" to reactivatedCrn,
          "UNMERGED_CRN" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
    }

    @Test
    fun `should create record when unmerged record not found`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      createPerson(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
        personKeyEntity = createPersonKey(),
      )

      val reactivated = ApiResponseSetup(crn = reactivatedCrn)
      val unmerged = ApiResponseSetup(crn = unmergedCrn)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

      checkTelemetry(
        UNMERGE_MESSAGE_RECEIVED,
        mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UNMERGE_RECORD_NOT_FOUND,
        mapOf("CRN" to unmergedCrn, "RECORD_TYPE" to UnmergeRecordType.UNMERGED.name, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "REACTIVATED_CRN" to reactivatedCrn,
          "UNMERGED_CRN" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
    }

    @Nested
    inner class DeletesARecord {
      @BeforeEach
      fun beforeEach() {
        stubDeletePersonMatch()
      }

      @Test
      fun `offender unmerge event is published`() {
        val reactivatedCrn = randomCrn()
        val unmergedCrn = randomCrn()

        val personKey = createPersonKey()
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
          personKeyEntity = personKey,
        )
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
          personKeyEntity = personKey,
        )

        val reactivated = ApiResponseSetup(crn = reactivatedCrn)
        val unmerged = ApiResponseSetup(crn = unmergedCrn)

        probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn)

        await.atMost(4, SECONDS) untilAsserted { assertThat(personRepository.findByCrn(reactivatedCrn)?.mergedTo).isNotNull() }

        probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

        checkTelemetry(
          UNMERGE_MESSAGE_RECEIVED,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_UUID_CREATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_UUID_CREATED,
          mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
          times = 0,
        )
        checkTelemetry(
          CPR_RECORD_UNMERGED,
          mapOf(
            "REACTIVATED_CRN" to reactivatedCrn,
            "UNMERGED_CRN" to unmergedCrn,
            "UNMERGED_UUID" to personKey.personUUID.toString(),
            "SOURCE_SYSTEM" to "DELIUS",
          ),
        )

        val unmergedPerson = awaitNotNullPerson { personRepository.findByCrn(unmergedCrn) }
        val reactivatedPerson = awaitNotNullPerson { personRepository.findByCrn(reactivatedCrn) }
        assertThat(unmergedPerson.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(unmergedPerson.personKey?.personEntities?.size).isEqualTo(1)
        assertThat(reactivatedPerson.personKey?.personEntities?.size).isEqualTo(1)
        assertThat(reactivatedPerson.personKey?.personUUID).isNotEqualTo(personKey.personUUID)
      }

      @Test
      fun `should mark unmerged UUID as needs attention if it has additional records`() {
        val reactivatedCrn = randomCrn()
        val unmergedCrn = randomCrn()

        val personKey = createPersonKey()
        createPerson(
          person = Person(
            crn = randomCrn(),
            sourceSystem = SourceSystemType.DELIUS,
          ),
          personKeyEntity = personKey,
        )
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
          personKeyEntity = personKey,
        )
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
          personKeyEntity = personKey,
        )

        val reactivated = ApiResponseSetup(crn = reactivatedCrn)
        val unmerged = ApiResponseSetup(crn = unmergedCrn)

        probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn)

        await.atMost(4, SECONDS) untilAsserted { assertThat(personRepository.findByCrn(reactivatedCrn)?.mergedTo).isNotNull() }

        probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

        checkTelemetry(
          UNMERGE_MESSAGE_RECEIVED,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_UNMERGE_LINK_NOT_FOUND,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "RECORD_TYPE" to UnmergeRecordType.REACTIVATED.name, "SOURCE_SYSTEM" to "DELIUS"),
          times = 0,
        )
        checkTelemetry(
          CPR_UUID_CREATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UNMERGED,
          mapOf(
            "REACTIVATED_CRN" to reactivatedCrn,
            "UNMERGED_CRN" to unmergedCrn,
            "UNMERGED_UUID" to personKey.personUUID.toString(),
            "SOURCE_SYSTEM" to "DELIUS",
          ),
        )

        awaitAssert {
          val personEntity = personRepository.findByCrn(unmergedCrn)
          assertThat(personEntity?.personKey?.status).isEqualTo(UUIDStatusType.NEEDS_ATTENTION)
        }
      }

      @Test
      fun `should remove link between records if existed`() {
        val reactivatedCrn = randomCrn()
        val unmergedCrn = randomCrn()

        val personKey = createPersonKey()
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
          personKeyEntity = personKey,
        )
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
          personKeyEntity = personKey,
        )

        val reactivated = ApiResponseSetup(crn = reactivatedCrn)
        val unmerged = ApiResponseSetup(crn = unmergedCrn)

        probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn)
        awaitAssert { assertThat(personRepository.findByCrn(reactivatedCrn)?.mergedTo).isNotNull() }
        probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

        checkTelemetry(
          UNMERGE_MESSAGE_RECEIVED,
          mapOf(
            "REACTIVATED_CRN" to reactivatedCrn,
            "UNMERGED_CRN" to unmergedCrn,
            "EVENT_TYPE" to OFFENDER_UNMERGED,
            "SOURCE_SYSTEM" to "DELIUS",
          ),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_UNMERGE_LINK_NOT_FOUND,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "RECORD_TYPE" to UnmergeRecordType.REACTIVATED.name, "SOURCE_SYSTEM" to "DELIUS"),
          times = 0,
        )

        val reactivatedPerson = awaitNotNullPerson { personRepository.findByCrn(reactivatedCrn) }
        assertThat(reactivatedPerson.mergedTo).isNull()
      }

      @Test
      fun `should remove link from merged UUID and find and assign to a new one`() {
        val reactivatedCrn = randomCrn()
        val unmergedCrn = randomCrn()

        val unmergedPersonKey = createPersonKey()
        val reactivatedPersonKey = createPersonKey()

        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
          personKeyEntity = unmergedPersonKey,
        )
        createPerson(
          Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
          personKeyEntity = reactivatedPersonKey,
        )

        val reactivated = ApiResponseSetup(crn = reactivatedCrn)
        val unmerged = ApiResponseSetup(crn = unmergedCrn)

        probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn)
        awaitAssert { assertThat(personRepository.findByCrn(reactivatedCrn)?.mergedTo).isNotNull() }
        probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

        checkTelemetry(
          UNMERGE_MESSAGE_RECEIVED,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_UNMERGE_LINK_NOT_FOUND,
          mapOf("REACTIVATED_CRN" to reactivatedCrn, "RECORD_TYPE" to UnmergeRecordType.REACTIVATED.name, "SOURCE_SYSTEM" to "DELIUS"),
          times = 0,
        )
        checkTelemetry(
          CPR_UUID_CREATED,
          mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        )
        checkTelemetry(
          CPR_RECORD_UNMERGED,
          mapOf(
            "REACTIVATED_CRN" to reactivatedCrn,
            "UNMERGED_CRN" to unmergedCrn,
            "UNMERGED_UUID" to unmergedPersonKey.personUUID.toString(),
            "SOURCE_SYSTEM" to "DELIUS",
          ),
        )

        val reactivatedMergedPersonKey = await.atMost(4, SECONDS) untilNotNull { personKeyRepository.findByPersonUUID(reactivatedPersonKey.personUUID) }
        assertThat(reactivatedMergedPersonKey.personEntities.size).isEqualTo(0)
        assertThat(reactivatedMergedPersonKey.status).isEqualTo(UUIDStatusType.MERGED)

        val reactivatedPerson = awaitNotNullPerson { personRepository.findByCrn(reactivatedCrn) }
        assertThat(reactivatedPerson.personKey?.personUUID).isNotEqualTo(reactivatedPersonKey.personUUID)
        assertThat(reactivatedPerson.personKey?.personUUID).isNotEqualTo(unmergedPersonKey.personUUID)
      }
    }
  }

  @Test
  fun `should log when message processing fails`() {
    val reactivatedCrn = randomCrn()
    val unmergedCrn = randomCrn()
    stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure")
    stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure", "next request will fail")
    stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure", "next request will fail")

    val messageId = publishDomainEvent(
      OFFENDER_UNMERGED,
      DomainEvent(
        eventType = OFFENDER_UNMERGED,
        additionalInformation = AdditionalInformation(
          reactivatedCrn = reactivatedCrn,
          unmergedCrn = unmergedCrn,
        ),
      ),
    )

    purgeQueueAndDlq(probationMergeEventsQueue)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        EventKeys.EVENT_TYPE.toString() to OFFENDER_UNMERGED,
        EventKeys.MESSAGE_ID.toString() to messageId,
      ),
    )
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val reactivatedCrn = randomCrn()
    val unmergedCrn = randomCrn()
    stub404Response(probationUrl(unmergedCrn))

    publishDomainEvent(
      OFFENDER_UNMERGED,
      DomainEvent(
        eventType = OFFENDER_UNMERGED,
        additionalInformation = AdditionalInformation(
          reactivatedCrn = reactivatedCrn,
          unmergedCrn = unmergedCrn,
        ),
      ),
    )

    expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)
    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }
}
