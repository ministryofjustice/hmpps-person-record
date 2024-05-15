package uk.gov.justice.digital.hmpps.personrecord.model

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
  fun getMessageType(): String? {
    return messageAttributes?.messageType?.value
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageAttributes(
  @JsonProperty(value = "messageType")
  val messageType: MessageType?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageType(
  @JsonProperty("Value")
  val value: String?,
)
