package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalInformation(

  @JsonProperty("sourceCRN")
  val sourceCrn: String? = null,

  @JsonProperty("targetCRN")
  val targetCrn: String? = null,

  @JsonProperty("reactivatedCRN")
  val reactivatedCrn: String? = null,

  @JsonProperty("unmergedCRN")
  val unmergedCrn: String? = null,

  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String? = null,

  @JsonProperty("addressId")
  val inboundDeliusAddressId: String? = null,

  val cprAddressId: String? = null,

  @JsonProperty("deliusAddressId")
  val outboundDeliusAddressId: String? = null,
)
