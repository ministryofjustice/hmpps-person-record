package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class PrisonAliasesAndIdentifiersRequest(
  val aliases: List<PrisonAlias>,
  val identifiers: List<PrisonIdentifier>,
)
