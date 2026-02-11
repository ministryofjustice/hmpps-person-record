package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Prisoner(
  @Schema(description = "The demographic attributes of the person")
  val demographicAttributes: DemographicAttributes,
  @Schema(description = "The aliases of the person")
  val aliases: List<Alias> = emptyList(),
  @Schema(description = "The addresses of the person")
  val addresses: List<Address> = emptyList(),
  @Schema(description = "The contacts of the person")
  val personContacts: List<Contact> = emptyList(),
  @Schema(description = "The sentence dates of the person")
  val sentences: List<Sentence> = emptyList(),
)
