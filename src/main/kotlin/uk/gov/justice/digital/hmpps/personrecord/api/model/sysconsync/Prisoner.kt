package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

// NOTE: Rename class and sub classes with "Syscon" Prefix to avoid having to use full qualifier name everywhere?!?
data class Prisoner(
  @Schema(description = "The names of the person")
  val name: Name,
  @Schema(description = "The demographic attributes of the person")
  val demographicAttributes: DemographicAttributes,
  @Schema(description = "The aliases of the person")
  val aliases: List<Alias> = emptyList(),
  @Schema(description = "The addresses of the person")
  val addresses: List<Address> = emptyList(),
  @Valid
  @Schema(description = "The contacts of the person")
  val contacts: List<Contact> = emptyList(),
  @Schema(description = "The identifiers of the person")
  val identifiers: List<Identifier> = emptyList(),
  @Schema(description = "The sentence dates of the person")
  val sentences: List<Sentence> = emptyList(),
)
