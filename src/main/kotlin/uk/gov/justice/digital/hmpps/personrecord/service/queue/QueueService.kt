package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID
import software.amazon.awssdk.services.sns.model.MessageAttributeValue as SNSMessageAttribute
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue as SQSMessageAttribute

@Component
class QueueService(
  private val hmppsQueueService: HmppsQueueService,
) {

  fun publishCourtMessageToFifoTopic(message: String, messageType: MessageType) {
    val topic = findByTopicIdOrThrow(Topics.COURT_EVENTS_FIFO.id)
    val messageBuilder = PublishRequest.builder()
      .topicArn(topic.arn)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to SNSMessageAttribute.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to SNSMessageAttribute.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      ).messageGroupId(UUID.randomUUID().toString())

    topic.snsClient.publish(messageBuilder.build())?.get()
  }

  fun publishReclusterMessageToQueue(uuid: UUID) {
    val queue = findByQueueIdOrThrow(Queues.RECLUSTER_EVENTS_QUEUE.id)
    val messageBuilder = SendMessageRequest.builder()
      .queueUrl(queue.queueUrl)
      .messageBody(uuid.toString())
      .messageAttributes(
        mapOf(
          "messageType" to SQSMessageAttribute.builder().dataType("String")
            .stringValue(NOTIFICATION).build(),
          "messageId" to SQSMessageAttribute.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      )

    queue.sqsClient.sendMessage(messageBuilder.build())
  }

  private fun findByTopicIdOrThrow(topicId: String) = hmppsQueueService.findByTopicId(topicId)
    ?: throw MissingTopicException("Could not find topic $topicId")

  private fun findByQueueIdOrThrow(queueId: String) = hmppsQueueService.findByQueueId(queueId)
    ?: throw MissingQueueException("Could not find queue $queueId")
}
