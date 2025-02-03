package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

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
}

data class MessageAttributes(
  val eventType: SQSEventType?,
  val messageType: SQSMessageType?,
)

data class SQSEventType(
  @JsonProperty("Value")
  val value: String?,
)

data class SQSMessageType(
  @JsonProperty("Value")
  val value: String?,
)
