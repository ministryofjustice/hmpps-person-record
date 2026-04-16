package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class UUIDStatusType {
  ACTIVE,
  MERGED,
  NEEDS_ATTENTION,

  // Deprecated status types. Still kept in enum so we can still decode old status types.
  @Deprecated("Clusters are no longer merged")
  RECLUSTER_MERGE,
}
