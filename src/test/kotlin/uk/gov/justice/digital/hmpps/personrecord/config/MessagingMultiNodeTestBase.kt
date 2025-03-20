package uk.gov.justice.digital.hmpps.personrecord.config

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.ProbationEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import uk.gov.justice.hmpps.sqs.publish
import java.util.UUID

@ExtendWith(MultiApplicationContextExtension::class)
abstract class MessagingMultiNodeTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcasestopic")
  }

  val courtEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.COURT_CASES_QUEUE_ID)
  }

  val probationEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_EVENT_QUEUE_ID)
  }

  val probationMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_MERGE_EVENT_QUEUE_ID)
  }

  val probationDeleteEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_DELETION_EVENT_QUEUE_ID)
  }

  val prisonEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PRISON_EVENT_QUEUE_ID)
  }

  val prisonMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PRISON_MERGE_EVENT_QUEUE_ID)
  }

  val reclusterEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.RECLUSTER_EVENTS_QUEUE_ID)
  }

  internal fun publishLibraMessage(message: String): String = publishCourtMessage(message, LIBRA_COURT_CASE, "libra.case.received")

  internal fun publishCommonPlatformMessage(message: String): String = publishCourtMessage(message, COMMON_PLATFORM_HEARING, "commonplatform.case.received")

  internal fun publishLargeCommonPlatformMessage(message: String): String {
    val request = SendMessageRequest.builder()
      .queueUrl(courtEventsQueue?.queueUrl)
      .messageDeduplicationId("dedube")
      .messageBody(message)
      .messageGroupId(COMMON_PLATFORM_HEARING.name)
      .messageAttributes(
        mapOf<String, software.amazon.awssdk.services.sqs.model.MessageAttributeValue>(
          "messageType" to software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(NOTIFICATION).build(),
          "eventType" to software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
            .dataType("String")
            .stringValue("commonplatform.case.received").build(),
          "messageId" to software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      )
      .build()

    val publishResponse = courtEventsQueue?.sqsClient?.sendMessage(request)

    expectNoMessagesOn(courtEventsQueue)
    return publishResponse!!.get().messageId()
  }

  private fun publishCourtMessage(message: String, messageType: MessageType, eventType: String): String {
    val publishResponse = courtEventsTopic?.publish(
      eventType = messageType.name,
      event = message,
      attributes = mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(messageType.name).build(),
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(eventType).build(),
        "messageId" to MessageAttributeValue.builder().dataType("String")
          .stringValue(UUID.randomUUID().toString()).build(),
      ),
      messageGroupId = messageType.name,
    )

    expectNoMessagesOn(courtEventsQueue)
    return publishResponse!!.messageId()
  }

  fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
    } matches { it == 0 }
  }

  fun expectNoMessagesOnQueueOrDlq(queue: HmppsQueue?) {
    expectNoMessagesOn(queue)
    await untilCallTo {
      queue?.sqsDlqClient?.countMessagesOnQueue(queue.dlqUrl!!)?.get()
    } matches { it == 0 }
  }

  fun publishDomainEvent(eventType: String, domainEvent: DomainEvent): String {
    val response = publishEvent(
      objectMapper.writeValueAsString(domainEvent),
      domainEventsTopic,
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(eventType).build(),
      ),
      eventType,
    )
    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOn(probationMergeEventsQueue)
    expectNoMessagesOn(prisonMergeEventsQueue)
    expectNoMessagesOn(prisonEventsQueue)
    return response!!
  }

  fun publishProbationEvent(eventType: String, probationEvent: ProbationEvent) {
    publishEvent(
      objectMapper.writeValueAsString(probationEvent),
      domainEventsTopic,
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(eventType).build(),
      ),
      eventType,
    )
    expectNoMessagesOn(probationEventsQueue)
  }

  private fun publishEvent(message: String, topic: HmppsTopic?, messageAttributes: Map<String, MessageAttributeValue>, eventType: String): String? = topic?.publish(event = message, eventType = eventType, attributes = messageAttributes)?.messageId()

  fun probationMergeEventAndResponseSetup(
    eventType: String,
    source: ApiResponseSetup,
    target: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubSingleProbationResponse(target, scenario, currentScenarioState, nextScenarioState)

    publishDomainEvent(
      eventType,
      DomainEvent(
        eventType = eventType,
        additionalInformation = AdditionalInformation(
          sourceCrn = source.crn,
          targetCrn = target.crn,
        ),
      ),
    )
  }

  fun probationUnmergeEventAndResponseSetup(
    eventType: String,
    reactivated: ApiResponseSetup,
    unmerged: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubSingleProbationResponse(unmerged, scenario, currentScenarioState, nextScenarioState)
    stubSingleProbationResponse(reactivated, scenario, currentScenarioState, nextScenarioState)

    publishDomainEvent(
      eventType,
      DomainEvent(
        eventType = eventType,
        additionalInformation = AdditionalInformation(
          reactivatedCrn = reactivated.crn,
          unmergedCrn = unmerged.crn,
        ),
      ),
    )
  }

  fun probationDomainEventAndResponseSetup(
    eventType: String,
    apiResponseSetup: ApiResponseSetup,
    additionalInformation: AdditionalInformation? = null,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubSingleProbationResponse(apiResponseSetup, scenario, currentScenarioState, nextScenarioState)

    val crnType = PersonIdentifier("CRN", apiResponseSetup.crn!!)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      personReference = personReference,
      additionalInformation = additionalInformation,
    )
    publishDomainEvent(eventType, domainEvent)
  }

  fun probationEventAndResponseSetup(eventType: String, apiResponseSetup: ApiResponseSetup, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) {
    stubSingleProbationResponse(apiResponseSetup, scenario, currentScenarioState, nextScenarioState)

    val probationEvent = ProbationEvent(apiResponseSetup.crn!!)
    publishProbationEvent(eventType, probationEvent)
  }

  fun prisonMergeEventAndResponseSetup(
    eventType: String,
    source: ApiResponseSetup,
    target: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubPrisonResponse(target, scenario, currentScenarioState, nextScenarioState)

    publishDomainEvent(
      eventType,
      DomainEvent(
        eventType = eventType,
        additionalInformation = AdditionalInformation(
          prisonNumber = target.prisonNumber,
          sourcePrisonNumber = source.prisonNumber,
        ),
      ),
    )
  }

  @BeforeEach
  fun beforeEachMessagingTest() {
    purgeQueueAndDlq(courtEventsQueue)
    purgeQueueAndDlq(probationEventsQueue)
    purgeQueueAndDlq(probationMergeEventsQueue)
    purgeQueueAndDlq(probationDeleteEventsQueue)
    purgeQueueAndDlq(prisonEventsQueue)
    purgeQueueAndDlq(prisonMergeEventsQueue)
    purgeQueueAndDlq(reclusterEventsQueue)
    expectNoMessagesOnQueueOrDlq(reclusterEventsQueue)
  }

  fun purgeQueueAndDlq(hmppsQueue: HmppsQueue?) {
    hmppsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsQueue.queueUrl).build(),
    ).get()
    hmppsQueue.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsQueue.dlqUrl).build(),
    ).get()
  }
}
