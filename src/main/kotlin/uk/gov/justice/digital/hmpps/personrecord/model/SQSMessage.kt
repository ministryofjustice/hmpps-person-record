package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import java.util.*

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
  fun getMessageType(): MessageType {
    return Optional.ofNullable(messageAttributes)
      .map<MessageType>(MessageAttributes::messageType)
      .orElse(MessageType.UNKNOWN)
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageAttributes(
  val messageType: MessageType? = null,
)
