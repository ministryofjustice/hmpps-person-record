package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.UnmergeService.Companion.UnmergeRecordType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_SELF_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_LINK_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS

class ProbationUnmergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  private fun probationUrl(crn: String) = "/probation-cases/$crn"

  @Test
  fun `offender unmerge event is published`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    val personKey = createPersonKey()
    val unmergedEntity = createPerson(
      person = Person(
        crn = unmergedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    val reactivatedEntity = createPerson(
      person = Person(
        crn = reactivatedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    mergeRecord(unmergedEntity, reactivatedEntity)

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "true",
        "PROBABILITY_SCORE" to "0.9999",
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
        "CRN" to reactivatedCrn,
      ),
    )
    checkTelemetry(
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "true",
        "PROBABILITY_SCORE" to "0.9999",
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
        "CRN" to unmergedCrn,
      ),
    )

    val unmergedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(unmergedCrn) }
    assertThat(unmergedPerson.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
  }

  @Test
  fun `should create record when unmerged record not found`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    createPerson(
      person = Person(
        crn = reactivatedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
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
      mapOf("UNMERGED_CRN" to unmergedCrn, "RECORD_TYPE" to UnmergeRecordType.UNMERGED.name, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  @Test
  fun `should create record when reactivated record not found and should not assign UUID`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    createPerson(
      person = Person(
        crn = unmergedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
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
      CPR_RECORD_CREATED,
      mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )

    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(reactivatedPerson.personKey).isNull()
  }

  @Test
  fun `should mark unmerged UUID as needs attention if it has additional records`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    val personKey = createPersonKey()

    createPerson(
      person = Person(
        crn = randomCRN(),
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    val unmergedEntity = createPerson(
      person = Person(
        crn = unmergedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    val reactivatedEntity = createPerson(
      person = Person(
        crn = reactivatedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    mergeRecord(unmergedEntity, reactivatedEntity)

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(unmergedCrn) }
    assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.NEEDS_ATTENTION)
  }

  @Test
  fun `should remove link between records if existed`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    val personKey = createPersonKey()
    val reactivatedEntity = createPerson(
      person = Person(
        crn = unmergedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    val unmergedEntity = createPerson(
      person = Person(
        crn = reactivatedCrn,
        sourceSystemType = SourceSystemType.DELIUS,
      ),
      personKeyEntity = personKey,
    )
    mergeRecord(unmergedEntity, reactivatedEntity)

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
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
      mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      times = 0,
    )

    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(reactivatedPerson.mergedTo).isNull()
  }

  @Test
  fun `should retry on 500 error`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub500Response(probationUrl(unmergedCrn), "next request will succeed", "retry")

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  @Test
  fun `should log when message processing fails`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")
    stub500Response(probationUrl(reactivatedCrn), STARTED, "failure")
    stub500Response(probationUrl(reactivatedCrn), STARTED, "failure")
    stub500Response(probationUrl(reactivatedCrn), STARTED, "failure")

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

    probationMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.queueUrl).build(),
    ).get()
    probationMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.dlqUrl).build(),
    ).get()

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
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub404Response(probationUrl(reactivatedCrn))
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

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }
}
