package uk.gov.justice.digital.hmpps.personrecord.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.service.type.RECLUSTER_EVENT
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.UUID
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue as SQSMessageAttribute

@Component
class QueueService(
  private val objectMapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService,
) {

  fun publishReclusterMessageToQueue(uuid: UUID) {
    val queue = findByQueueIdOrThrow(Queues.RECLUSTER_EVENTS_QUEUE_ID)
    val message = objectMapper.writeValueAsString(
      Recluster(uuid = uuid.toString()),
    )
    val messageBuilder = SendMessageRequest.builder()
      .queueUrl(queue.queueUrl)
      .messageBody(message)
      .messageAttributes(
        mapOf(
          sqsAttribute("eventType", RECLUSTER_EVENT),
          sqsAttribute("messageType", NOTIFICATION),
          sqsAttribute("messageId", UUID.randomUUID().toString()),
        ),
      )

    queue.sqsClient.sendMessage(messageBuilder.build())
  }

  private fun sqsAttribute(key: String, value: String): Pair<String, MessageAttributeValue> = key to SQSMessageAttribute.builder().dataType("String")
    .stringValue(value).build()

  private fun findByQueueIdOrThrow(queueId: String) = hmppsQueueService.findByQueueId(queueId)
    ?: throw MissingQueueException("Could not find queue $queueId")
}
