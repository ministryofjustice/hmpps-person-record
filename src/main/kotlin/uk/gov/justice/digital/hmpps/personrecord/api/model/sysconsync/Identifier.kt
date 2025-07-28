package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class Identifier(
  val type: IdentifierType,
  val value: String?,
)

enum class IdentifierType {
  PNC,
  CRO,
  NINO,
  DL,
}
