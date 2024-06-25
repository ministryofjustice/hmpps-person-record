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
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNINumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ProbationCaseResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID

@ExtendWith(MultiApplicationContextExtension::class)
abstract class MessagingMultiNodeTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtCaseEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }

  val courtCaseEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }

  val probationEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  private val prisonEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprnomiseventsqueue")
  }

  internal fun publishHMCTSMessage(message: String, messageType: MessageType): String {
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to MessageAttributeValue.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      )
      .build()

    val response: PublishResponse? = courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    expectNoMessagesOn(courtCaseEventsQueue)
    return response!!.messageId()
  }

  private fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
    } matches { it == 0 }
  }

  fun publishDomainEvent(eventType: String, domainEvent: DomainEvent) {
    val domainEventAsString = objectMapper.writeValueAsString(domainEvent)
    val publishRequest = PublishRequest.builder().topicArn(domainEventsTopic?.arn)
      .message(domainEventAsString)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(eventType).build(),
        ),
      ).build()

    domainEventsTopic?.snsClient?.publish(publishRequest)?.get()

    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOn(prisonEventsQueue)
  }

  fun probationDomainEventAndResponseSetup(eventType: String, pnc: String?, crn: String = randomCRN(), cro: String = randomCro(), additionalInformation: AdditionalInformation? = null, prisonNumber: String = "", prefix: String = randomFirstName(), scenario: String = "anyScenario", currentScenarioState: String = STARTED, nextScenarioState: String = STARTED): String {
    val probationCaseResponseSetup = ProbationCaseResponseSetup(
      crn = crn,
      pnc = pnc,
      cro = cro,
      prefix = prefix,
      prisonNumber = prisonNumber,
      nationalInsuranceNumber = randomNINumber(),
    )
    stubSingleProbationResponse(probationCaseResponseSetup, scenario, currentScenarioState, nextScenarioState)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      detailUrl = createDeliusDetailUrl(crn),
      personReference = personReference,
      additionalInformation = additionalInformation,
    )
    publishDomainEvent(eventType, domainEvent)
    return crn
  }
  private fun stubSingleProbationResponse(probationCase: ProbationCaseResponseSetup, scenario: String, currentScenarioState: String, nextScenarioState: String) {
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

  fun stubMatchScore(matchResponse: MatchResponse) {
    wiremock.stubFor(
      WireMock.post("/person/match")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }

  fun createDeliusDetailUrl(crn: String): String =
    "https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/$crn"

  fun createNomsDetailUrl(prisonNumber: String): String =
    "https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner/$prisonNumber"

  @BeforeEach
  fun beforeEachMessagingTest() {
    courtCaseEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue!!.dlqUrl).build(),
    ).get()
    courtCaseEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue!!.queueUrl).build(),
    ).get()
    probationEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.queueUrl).build(),
    )
    probationEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.dlqUrl).build(),
    )
    prisonEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.queueUrl).build(),
    )
    prisonEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.dlqUrl).build(),
    )
  }
}
