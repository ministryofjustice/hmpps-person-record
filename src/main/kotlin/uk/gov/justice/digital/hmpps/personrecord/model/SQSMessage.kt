package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

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
)

data class MessageAttributes(
  @JsonProperty("Value")
  val value: MessageType? = null,
)
