package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class PrisonAliasesAndIdentifiersRequest(
  @Valid
  @NotEmpty
  val aliases: List<PrisonAlias>,
  @Valid
  @NotEmpty
  val identifiers: List<Identifier>,
)
