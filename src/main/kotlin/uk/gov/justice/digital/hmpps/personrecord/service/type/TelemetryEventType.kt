package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_CANDIDATE_RECORD_SEARCH("CprCandidateRecordSearch"),
  CPR_CANDIDATE_RECORD_FOUND_UUID("CprSplinkCandidateRecordsFoundGetUUID"),
  CPR_UUID_CREATED("CprUuidCreated"),
  CPR_RECORD_MERGED("CprRecordMerged"),
  CPR_UUID_MERGED("CprUuidMerged"),
  CPR_RECORD_UNMERGED("CprRecordUnmerged"),
  CPR_RECORD_DELETED("CprRecordHardDeleted"),
  CPR_UUID_DELETED("CprUUIDHardDeleted"),
  CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED("CprReclusterClusterRecordsNotLinked"),
  CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS("CprReclusterMatchedClustersHasExclusions"),
  CPR_RECLUSTER_SELF_HEALED("CprReclusterSelfHealed"),
  CPR_RECLUSTER_MERGE("CprReclusterMerge"),
  CPR_RECORD_COUNT_REPORT("CprRecordCountReport"),
  CPR_ADMIN_RECLUSTER_TRIGGERED("CprAdminReclusterTriggered"),
}
