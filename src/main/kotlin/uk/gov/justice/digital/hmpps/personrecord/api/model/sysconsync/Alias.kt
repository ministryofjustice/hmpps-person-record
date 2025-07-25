package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class Alias(
  val id: String?,
  val titleCode: String?,
  val firstName: String?,
  val middleName1: String?,
  val middleName2: String?,
  val lastName: String?,
  val type: AliasType?,
)

enum class AliasType {
  A,
  CN,
  MAID,
  NICK
}
