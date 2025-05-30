package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personrecord.service.queue.LARGE_CASE_EVENT_TYPE

@JsonIgnoreProperties(ignoreUnknown = true)
data class SQSMessage(
  @NotBlank
  @JsonProperty("Type")
  val type: String,
  @NotBlank
  @JsonProperty("Message")
  val message: String,
  @JsonProperty("MessageId")
  val messageId: String? = null,
  @JsonProperty(value = "MessageAttributes")
  val messageAttributes: MessageAttributes? = null,
) {
  fun getMessageType(): String? = messageAttributes?.messageType?.value
  fun getEventType(): String? = messageAttributes?.eventType?.value
  fun getHearingEventType(): String? = messageAttributes?.hearingEventType?.value
  fun isLargeMessage() = getEventType() == LARGE_CASE_EVENT_TYPE
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageAttributes(
  val eventType: MessageAttribute?,
  val messageType: MessageAttribute?,
  val hearingEventType: MessageAttribute?,
)

data class MessageAttribute(
  @JsonProperty("Value")
  val value: String?,
)
