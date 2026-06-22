package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class SourceSystemType(val description: String) {
  NOMIS("prison"),
  DELIUS("probation"),
  COMMON_PLATFORM("commonplatform"),
  LIBRA("libra"),
}
