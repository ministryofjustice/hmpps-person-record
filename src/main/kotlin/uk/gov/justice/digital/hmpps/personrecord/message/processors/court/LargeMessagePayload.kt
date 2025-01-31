package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class LargeMessagePayload(
  @NotBlank
  @JsonProperty("software.amazon.payloadoffloading.PayloadS3Pointer")
  val pointer: LargeMessagePointer,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LargeMessagePointer(
  @NotBlank
  val s3Key: String,
  @NotBlank
  val s3Bucket: String,
)
