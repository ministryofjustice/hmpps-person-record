package uk.gov.justice.digital.hmpps.personrecord.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService

abstract class MessagingSingleNodeTestBase : IntegrationTestBase() {

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

    return response!!.messageId()
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
  }

  fun createDeliusDetailUrl(crn: String): String =
    "https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/$crn"

  fun createNomsDetailUrl(nomsNumber: String): String =
    "https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner/$nomsNumber"

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
