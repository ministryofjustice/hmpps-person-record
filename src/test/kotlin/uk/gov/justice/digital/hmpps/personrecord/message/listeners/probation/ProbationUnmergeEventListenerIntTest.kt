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
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
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
import uk.gov.justice.digital.hmpps.personrecord.service.person.UnmergeService.Companion.UnmergeRecordType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_LINK_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
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

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivated, unmerged)
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
      TelemetryEventType.CPR_RECORD_UNMERGED,
      mapOf(
        "REACTIVATED_CRN" to reactivatedCrn,
        "UNMERGED_CRN" to unmergedCrn,
        "UNMERGED_UUID" to personKey.personId.toString(),
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )

    val unmergedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(unmergedCrn) }
    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(unmergedPerson.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
    assertThat(unmergedPerson.personKey?.personEntities?.size).isEqualTo(1)
    assertThat(reactivatedPerson.personKey?.personEntities?.size).isEqualTo(1)
    assertThat(reactivatedPerson.personKey?.personId).isNotEqualTo(personKey.personId)
  }

  @Test
  fun `should create record when unmerged record not found`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = reactivatedCrn))),
      personKeyEntity = createPersonKey(),
    )

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

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
  fun `should create record when reactivated record not found and should create a UUID`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = unmergedCrn))),
      personKeyEntity = personKeyEntity,
    )

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )

    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(reactivatedPerson.personKey).isNotNull()
    assertThat(reactivatedPerson.personKey?.personId).isNotEqualTo(personKeyEntity.personId)
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

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.9999999,
        "2" to 0.9999999,
      ),
    )
    stubMatchScore(matchResponse)

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivated, unmerged)
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

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivated, unmerged)
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

    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(reactivatedPerson.mergedTo).isNull()
  }

  @Test
  fun `should remove link from merged UUID and find and assign to a new one`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()

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

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.9999999,
      ),
    )
    stubMatchScore(matchResponse)

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivated, unmerged)
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
      TelemetryEventType.CPR_RECORD_UNMERGED,
      mapOf(
        "REACTIVATED_CRN" to reactivatedCrn,
        "UNMERGED_CRN" to unmergedCrn,
        "UNMERGED_UUID" to unmergedPersonKey.personId.toString(),
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )

    val reactivatedMergedPersonKey = await.atMost(10, SECONDS) untilNotNull { personKeyRepository.findByPersonId(reactivatedPersonKey.personId) }
    assertThat(reactivatedMergedPersonKey.personEntities.size).isEqualTo(0)
    assertThat(reactivatedMergedPersonKey.status).isEqualTo(UUIDStatusType.MERGED)

    val reactivatedPerson = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(reactivatedCrn) }
    assertThat(reactivatedPerson.personKey?.personId).isNotEqualTo(reactivatedPersonKey.personId)
    assertThat(reactivatedPerson.personKey?.personId).isNotEqualTo(unmergedPersonKey.personId)
  }

  @Test
  fun `should retry on 500 error`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub500Response(probationUrl(unmergedCrn), "next request will succeed", "retry")

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

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
