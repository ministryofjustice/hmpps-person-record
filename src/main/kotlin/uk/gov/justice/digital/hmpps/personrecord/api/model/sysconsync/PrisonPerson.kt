package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class PrisonPerson(
  @Valid
  @NotEmpty
  val aliases: List<PrisonAlias>,
  val identifiers: List<PrisonIdentifier>,
)
