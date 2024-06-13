package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
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
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
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

  val offenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  val prisonerEventsQueue by lazy {
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
            .stringValue("d3242a9f-c1cd-4d16-bd46-b7d33ccc9849").build(),
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

    expectNoMessagesOn(offenderEventsQueue)
    expectNoMessagesOn(prisonerEventsQueue)
  }

  fun probationDomainEventAndResponseSetup(eventType: String, pnc: String?, crn: String = UUID.randomUUID().toString(), additionalInformation: AdditionalInformation? = null, prisonNumber: String = UUID.randomUUID().toString()): String {
    val probationCaseResponseSetup = ProbationCaseResponseSetup(crn = crn, pnc = pnc, prefix = "POPOne", prisonNumber = prisonNumber)
    stubSingleProbationResponse(probationCaseResponseSetup)

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
  private fun stubSingleProbationResponse(probationCase: ProbationCaseResponseSetup) {
    wiremock.stubFor(
      WireMock.get("/probation-cases/${probationCase.crn}")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(probationCaseResponse(probationCase))
            .withStatus(200),
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
    offenderEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(offenderEventsQueue!!.queueUrl).build(),
    )
    offenderEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(offenderEventsQueue!!.dlqUrl).build(),
    )
    prisonerEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonerEventsQueue!!.queueUrl).build(),
    )
    prisonerEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonerEventsQueue!!.dlqUrl).build(),
    )
  }
}
