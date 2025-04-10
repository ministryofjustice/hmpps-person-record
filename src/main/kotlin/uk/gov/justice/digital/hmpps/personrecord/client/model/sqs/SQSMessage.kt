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
  var message: String,
  @JsonProperty("MessageId")
  val messageId: String? = null,
  @JsonProperty(value = "MessageAttributes")
  val messageAttributes: MessageAttributes? = null,
) {
  fun getMessageType(): String? = messageAttributes?.messageType?.value
  fun getEventType(): String? = messageAttributes?.eventType?.value
  fun getHearingEventType(): String? = messageAttributes?.hearingEventType?.value
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
