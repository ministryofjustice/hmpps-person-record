package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration

class ProbationMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  private fun probationUrl(crn: String) = "/probation-cases/$crn"

  @Test
  fun `processes offender merge event with records with same UUID is published`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    val source = ApiResponseSetup(crn = sourceCrn)
    val target = ApiResponseSetup(crn = targetCrn)
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = sourceCrn))),
      personKeyEntity = personKeyEntity,
    )
    val targetPerson = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = targetCrn))),
      personKeyEntity = personKeyEntity,
    )

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, source, target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to personKeyEntity.personId.toString(),
        "FROM_UUID" to personKeyEntity.personId.toString(),
        "TO_SOURCE_SYSTEM" to "DELIUS",
        "FROM_SOURCE_SYSTEM" to "DELIUS",
      ),
    )

    val sourcePerson = personRepository.findByCrn(sourceCrn)
    assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    stub404Response(probationUrl(targetCrn))

    publishDomainEvent(
      OFFENDER_MERGED,
      DomainEvent(
        eventType = OFFENDER_MERGED,
        additionalInformation = AdditionalInformation(
          sourceCrn = sourceCrn,
          targetCrn = targetCrn,
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
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  @Test
  fun `should retry on 500 error`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    stub500Response(probationUrl(targetCrn), "next request will succeed", "retry")

    val source = ApiResponseSetup(crn = sourceCrn)
    val target = ApiResponseSetup(crn = targetCrn)
    probationMergeEventAndResponseSetup(OFFENDER_MERGED, source, target, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )

    //  Add check to make sure record updated once added CPR-338
  }

  @Test
  fun `should log when message processing fails`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    stub500Response(probationUrl(targetCrn), STARTED, "failure")
    stub500Response(probationUrl(targetCrn), STARTED, "failure")
    stub500Response(probationUrl(targetCrn), STARTED, "failure")

    val messageId = publishDomainEvent(
      OFFENDER_MERGED,
      DomainEvent(
        eventType = OFFENDER_MERGED,
        additionalInformation = AdditionalInformation(
          sourceCrn = sourceCrn,
          targetCrn = targetCrn,
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
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        EventKeys.EVENT_TYPE.toString() to OFFENDER_MERGED,
        EventKeys.MESSAGE_ID.toString() to messageId,
      ),
    )
  }
}
