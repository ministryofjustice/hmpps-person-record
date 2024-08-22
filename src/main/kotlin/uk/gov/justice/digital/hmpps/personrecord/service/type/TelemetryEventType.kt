package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  FIFO_DEFENDANT_RECEIVED("CprDefendantEventReceived"),
  FIFO_HEARING_CREATED("CprFIFOHearingCreated"),
  FIFO_HEARING_UPDATED("CprFIFOHearingUpdated"),
  MESSAGE_RECEIVED("CprDomainEventReceived"),
  MERGE_MESSAGE_RECEIVED("CprMergeEventReceived"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  MESSAGE_PROCESSING_FAILED("CprMessageProcessingFailed"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_UPDATE_RECORD_DOES_NOT_EXIST("CprUpdateRecordDoesNotExist"),
  CPR_NEW_RECORD_EXISTS("CprNewRecordExists"),
  CPR_CANDIDATE_RECORD_SEARCH("CprCandidateRecordSearch"),
  CPR_MATCH_SCORE("CprMatchScore"),
  CPR_MATCH_PERSON_DUPLICATE("CprMatchPersonRecordDuplicate"),
  CPR_CANDIDATE_RECORD_FOUND_UUID("CprSplinkCandidateRecordsFoundGetUUID"),
  CPR_UUID_CREATED("CprUuidCreated"),
  CPR_SELF_MATCH("CprSplinkSelfMatch"),
  CPR_LOW_SELF_SCORE_NOT_CREATING_UUID("CprSplinkSelfMatchNotCreatingUuid"),
  CPR_RECORD_MERGED("CprRecordMerged"),
  CPR_MERGE_RECORD_NOT_FOUND("CprMergeRecordNotFound"),
}
