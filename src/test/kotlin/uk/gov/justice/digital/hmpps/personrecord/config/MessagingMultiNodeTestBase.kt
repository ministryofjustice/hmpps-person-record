package uk.gov.justice.digital.hmpps.personrecord.config

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.ProbationEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration
import java.util.UUID

@ExtendWith(MultiApplicationContextExtension::class)
abstract class MessagingMultiNodeTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }

  val courtEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.COURT_CASE_EVENTS_QUEUE_ID)
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

  internal fun publishCourtMessage(message: String, messageType: MessageType, topic: String = courtEventsTopic?.arn!!): String {
    var messageBuilder = PublishRequest.builder()
      .topicArn(topic)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to MessageAttributeValue.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      )
    if (topic.contains(".fifo")) {
      messageBuilder = messageBuilder.messageGroupId(UUID.randomUUID().toString())
    }

    val response: PublishResponse? = courtEventsTopic?.snsClient?.publish(messageBuilder.build())?.get()

    expectNoMessagesOn(courtEventsQueue)
    return response!!.messageId()
  }

  fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
    } matches { it == 0 }
  }

  fun expectNoMessagesOnDlq(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsDlqClient?.countMessagesOnQueue(queue.dlqUrl!!)?.get()
    } matches { it == 0 }
  }

  fun publishDomainEvent(eventType: String, domainEvent: DomainEvent): String {
    val domainEventAsString = objectMapper.writeValueAsString(domainEvent)
    val response = publishEvent(
      domainEventAsString,
      domainEventsTopic,
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(eventType).build(),
      ),
    )
    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOn(probationMergeEventsQueue)
    expectNoMessagesOn(prisonMergeEventsQueue)
    expectNoMessagesOn(prisonEventsQueue)
    return response!!.messageId()
  }

  fun publishProbationEvent(eventType: String, probationEvent: ProbationEvent): String {
    val probationEventAsString = objectMapper.writeValueAsString(probationEvent)
    val response = publishEvent(
      probationEventAsString,
      domainEventsTopic,
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(eventType).build(),
      ),
    )
    expectNoMessagesOn(probationEventsQueue)
    return response!!.messageId()
  }

  private fun publishEvent(message: String, topic: HmppsTopic?, messageAttributes: Map<String, MessageAttributeValue>): PublishResponse? {
    val publishRequest = PublishRequest.builder().topicArn(topic?.arn)
      .message(message)
      .messageAttributes(messageAttributes).build()
    return topic?.snsClient?.publish(publishRequest)?.get()
  }

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

    val crnType = apiResponseSetup.crn?.let { PersonIdentifier("CRN", it) }
    val personReference = PersonReference(crnType?.let { listOf(crnType) } ?: listOf())

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

  private fun stubSingleProbationResponse(probationCase: ApiResponseSetup, scenario: String, currentScenarioState: String, nextScenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/probation-cases/${probationCase.crn}")
        .inScenario(scenario)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(probationCaseResponse(probationCase))
            .withStatus(200),
        ),
    )
  }

  fun stub404Response(url: String) {
    wiremock.stubFor(
      WireMock.get(url)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  fun stub500Response(url: String, nextScenarioState: String = "Next request will succeed", scenarioName: String) {
    wiremock.stubFor(
      WireMock.get(url)
        .inScenario(scenarioName)
        .whenScenarioStateIs(STARTED)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubPrisonResponse(
    apiResponseSetup: ApiResponseSetup,
    scenarioName: String? = BASE_SCENARIO,
    currentScenarioState: String? = STARTED,
    nextScenarioState: String? = STARTED,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/${apiResponseSetup.prisonNumber}")
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(apiResponseSetup)),
        ),
    )
  }

  fun blitz(actionCount: Int, threadCount: Int, action: () -> Unit) {
    val blitzer = Blitzer(actionCount, threadCount)
    try {
      blitzer.blitz {
        action()
      }
    } finally {
      blitzer.shutdown()
    }
  }

  @BeforeEach
  fun beforeEachMessagingTest() {
    courtEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsQueue!!.dlqUrl).build(),
    ).get()
    courtEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsQueue!!.queueUrl).build(),
    ).get()
    probationEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.queueUrl).build(),
    )
    probationEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.dlqUrl).build(),
    )
    probationMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.queueUrl).build(),
    )
    probationMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.dlqUrl).build(),
    )
    probationDeleteEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationDeleteEventsQueue!!.queueUrl).build(),
    )
    probationDeleteEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationDeleteEventsQueue!!.dlqUrl).build(),
    )
    prisonEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.queueUrl).build(),
    )
    prisonEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.dlqUrl).build(),
    )

    prisonMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.queueUrl).build(),
    )
    prisonMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.dlqUrl).build(),
    )

    reclusterEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(reclusterEventsQueue!!.queueUrl).build(),
    )
    reclusterEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(reclusterEventsQueue!!.dlqUrl).build(),
    )

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      reclusterEventsQueue!!.sqsDlqClient!!.countAllMessagesOnQueue(reclusterEventsQueue!!.dlqUrl!!).get()
    } matches { it == 0 }
  }
}
