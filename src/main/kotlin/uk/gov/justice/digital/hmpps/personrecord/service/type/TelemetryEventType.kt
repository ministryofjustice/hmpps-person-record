package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  MESSAGE_RECEIVED("CprDomainEventReceived"),
  MERGE_MESSAGE_RECEIVED("CprMergeEventReceived"),
  UNMERGE_MESSAGE_RECEIVED("CprUnmergeEventReceived"),
  MESSAGE_PROCESSING_FAILED("CprMessageProcessingFailed"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_CANDIDATE_RECORD_SEARCH("CprCandidateRecordSearch"),
  CPR_CANDIDATE_RECORD_FOUND_UUID("CprSplinkCandidateRecordsFoundGetUUID"),
  CPR_UUID_CREATED("CprUuidCreated"),
  CPR_RECORD_MERGED("CprRecordMerged"),
  CPR_MERGE_RECORD_NOT_FOUND("CprMergeRecordNotFound"),
  CPR_UNMERGE_RECORD_NOT_FOUND("CprUnmergeRecordNotFound"),
  CPR_UNMERGE_LINK_NOT_FOUND("CprUnmergeLinkNotFound"),
  CPR_RECORD_UNMERGED("CprRecordUnmerged"),
  CPR_RECORD_DELETED("CprRecordHardDeleted"),
  CPR_UUID_DELETED("CprUUIDHardDeleted"),
  CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION("CprUuidReclusterNeedsAttention"),
  CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED("CprReclusterClusterRecordsNotLinked"),
  CPR_RECLUSTER_NO_MATCH_FOUND("CprReclusterNoMatchFound"),
  CPR_RECLUSTER_MATCH_FOUND_MERGE("CprReclusterMatchFoundMergeRecluster"),
}
