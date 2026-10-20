package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonMerge(
  @Schema(description = "The prison number of the person being merged from", example = "A1234BC")
  val fromPrisonNumber: String,
  // TODO once the calling code is providing these details then make this non-nullable
  @Schema(description = "The merged addresses and contacts to overwrite on the person being merged to")
  val toPrisonAddressesAndContactsRequest: PrisonAddressesAndContactsRequest? = null,
)
