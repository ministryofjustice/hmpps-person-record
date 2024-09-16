package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.ProbationEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomNINumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
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
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }

  internal val courtEventsFIFOTopic by lazy {
    hmppsQueueService.findByTopicId("courteventsfifotopic")
  }

  val courtEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }

  val courtEventsTemporaryQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventstemporaryqueue")
  }

  val courtEventsFIFOQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourteventsfifoqueue")
  }

  val probationEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  val probationMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusmergeeventsqueue")
  }

  val prisonEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprnomiseventsqueue")
  }

  val prisonMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprnomismergeeventsqueue")
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

  private fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
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

  fun probationDomainEventAndResponseSetup(
    eventType: String,
    pnc: String?,
    crn: String = randomCRN(),
    cro: String = randomCro(),
    additionalInformation: AdditionalInformation? = null,
    prisonNumber: String = "",
    prefix: String = randomName(),
    addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(postcode = "LS1 1AB", fullAddress = "abc street")),
    sentences: List<ApiResponseSetupSentences> = listOf(ApiResponseSetupSentences(randomDate())),
    ethnicity: String = randomEthnicity(),
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ): String {
    val probationCaseResponseSetup = ApiResponseSetup(
      crn = crn,
      cro = cro,
      pnc = pnc,
      prefix = prefix,
      ethnicity = ethnicity,
      prisonNumber = prisonNumber,
      addresses = addresses,
      nationalInsuranceNumber = randomNINumber(),
      sentences = sentences,
    )
    stubSingleProbationResponse(probationCaseResponseSetup, scenario, currentScenarioState, nextScenarioState)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      personReference = personReference,
      additionalInformation = additionalInformation,
    )
    publishDomainEvent(eventType, domainEvent)
    return crn
  }

  fun probationEventAndResponseSetup(eventType: String, pnc: String?, crn: String = randomCRN(), cro: String = randomCro(), additionalInformation: AdditionalInformation? = null, prisonNumber: String = "", prefix: String = randomName(), addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(postcode = "LS1 1AB", fullAddress = "abc street")), ethnicity: String = randomEthnicity(), scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED): String {
    val probationCaseResponseSetup = ApiResponseSetup(
      crn = crn,
      cro = cro,
      pnc = pnc,
      prefix = prefix,
      prisonNumber = prisonNumber,
      addresses = addresses,
      ethnicity = ethnicity,
      nationalInsuranceNumber = randomNINumber(),
    )
    stubSingleProbationResponse(probationCaseResponseSetup, scenario, currentScenarioState, nextScenarioState)

    val probationEvent = ProbationEvent(crn)
    publishProbationEvent(eventType, probationEvent)
    return crn
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

  fun stubMatchScore(matchResponse: MatchResponse, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) {
    wiremock.stubFor(
      WireMock.post("/person/match")
        .inScenario(scenario)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }

  fun stubSelfMatchScore(score: Double = 0.9999, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) = stubMatchScore(
    matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf("0" to score),
    ),
    scenario = scenario,
    currentScenarioState = currentScenarioState,
    nextScenarioState = nextScenarioState,
  )

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

  @BeforeEach
  fun beforeEachMessagingTest() {
    courtEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsQueue!!.dlqUrl).build(),
    ).get()
    courtEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsQueue!!.queueUrl).build(),
    ).get()
    courtEventsTemporaryQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsTemporaryQueue!!.queueUrl).build(),
    ).get()
    courtEventsFIFOQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtEventsFIFOQueue!!.queueUrl).build(),
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

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue!!.sqsClient.countAllMessagesOnQueue(probationEventsQueue!!.queueUrl).get()
    } matches { it == 0 }
    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue!!.sqsDlqClient!!.countAllMessagesOnQueue(probationEventsQueue!!.dlqUrl!!).get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue!!.sqsClient.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl).get()
    } matches { it == 0 }
    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue!!.sqsDlqClient!!.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!).get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonEventsQueue!!.sqsClient.countAllMessagesOnQueue(prisonEventsQueue!!.queueUrl).get()
    } matches { it == 0 }
    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonEventsQueue!!.sqsDlqClient!!.countAllMessagesOnQueue(prisonEventsQueue!!.dlqUrl!!).get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue!!.sqsClient.countAllMessagesOnQueue(prisonMergeEventsQueue!!.queueUrl).get()
    } matches { it == 0 }
    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue!!.sqsDlqClient!!.countAllMessagesOnQueue(prisonMergeEventsQueue!!.dlqUrl!!).get()
    } matches { it == 0 }

    stubSelfMatchScore()
  }

  companion object {
    private const val BASE_SCENARIO = "baseScenario"
  }
}
