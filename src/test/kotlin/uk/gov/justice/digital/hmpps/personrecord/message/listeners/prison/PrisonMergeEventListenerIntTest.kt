package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration

class PrisonMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  @Test
  fun `processes prisoner merge event with records with same UUID is published`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(prisonNumber = sourcePrisonNumber, sourceSystemType = SourceSystemType.NOMIS),
      personKeyEntity = personKeyEntity,
    )
    val targetPerson = createPerson(
      Person(prisonNumber = targetPrisonNumber, sourceSystemType = SourceSystemType.NOMIS),
      personKeyEntity = personKeyEntity,
    )

    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source = source, target = target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to personKeyEntity.personId.toString(),
        "FROM_UUID" to personKeyEntity.personId.toString(),
        "TO_SOURCE_SYSTEM" to "NOMIS",
        "FROM_SOURCE_SYSTEM" to "NOMIS",
      ),
    )

    val sourcePerson = personRepository.findByPrisonNumberAndSourceSystem(sourcePrisonNumber)
    assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
  }

  @Test
  fun `processes prisoner merge event when target record does not exist`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(prisonNumber = sourcePrisonNumber, sourceSystemType = SourceSystemType.NOMIS),
      personKeyEntity = personKeyEntity,
    )

    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source = source, target = target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
    checkTelemetry(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        "RECORD_TYPE" to "TARGET",
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "FROM_SOURCE_SYSTEM" to "NOMIS",
        "TO_SOURCE_SYSTEM" to "NOMIS",
      ),
    )
  }

  @Test
  fun `processes prisoner merge event when source record does not exist`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(prisonNumber = targetPrisonNumber, sourceSystemType = SourceSystemType.NOMIS),
      personKeyEntity = personKeyEntity,
    )

    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source = source, target = target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
    checkTelemetry(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        "RECORD_TYPE" to "SOURCE",
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "FROM_SOURCE_SYSTEM" to "NOMIS",
        "TO_SOURCE_SYSTEM" to "NOMIS",
      ),
    )
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to personKeyEntity.personId.toString(),
        "FROM_UUID" to null,
        "TO_SOURCE_SYSTEM" to "NOMIS",
        "FROM_SOURCE_SYSTEM" to null,
      ),
    )
  }

  @Test
  fun `should retry on 500 error`() {
    val sourcePrisonNumber = randomPrisonNumber()
    val targetPrisonNumber = randomPrisonNumber()
    stub500Response(prisonURL(targetPrisonNumber), "next request will succeed", "retry")

    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source, target, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
  }

  @Test
  fun `should log when message processing fails`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")

    val additionalInformation =
      AdditionalInformation(prisonNumber = targetPrisonNumber, sourcePrisonNumber = sourcePrisonNumber)
    val domainEvent =
      DomainEvent(eventType = PRISONER_MERGED, personReference = null, additionalInformation = additionalInformation)
    val messageId = publishDomainEvent(PRISONER_MERGED, domainEvent)

    prisonMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.queueUrl).build(),
    ).get()
    prisonMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.dlqUrl).build(),
    ).get()
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "EVENT_TYPE" to PRISONER_MERGED,
        "SOURCE_SYSTEM" to "NOMIS",
      ),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to "NOMIS",
        "EVENT_TYPE" to "prisoner-offender-events.prisoner.merged",
      ),
    )
  }
}