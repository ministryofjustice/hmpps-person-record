package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class PrisonRecordType {
  CURRENT,
  HISTORIC,
  ;

  companion object {
    fun from(status: Boolean) = when (status) {
      true -> CURRENT
      false -> HISTORIC
    }
  }
}
