package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class PrisonRecordType(val value: Boolean) {
  CURRENT(true),
  HISTORIC(false),
  ;

  companion object {
    fun from(status: Boolean) = when (status) {
      true -> CURRENT
      false -> HISTORIC
    }
  }
}
