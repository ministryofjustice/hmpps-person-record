package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class UUIDStatusReasonType(
  val description: String,
) {
  BROKEN_CLUSTER(description = "Some records within the cluster no longer meet the similarity threshold."),
  OVERRIDE_CONFLICT(description = "The cluster has tried to join other clusters that had been intentionally excluded from each other."),
}
