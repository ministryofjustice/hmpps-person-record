package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  DOMAIN_EVENT_RECEIVED("CprDomainEventReceived"),
  COURT_MESSAGE_RECEIVED("CprCourtMessageReceived"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  MESSAGE_PROCESSING_FAILED("CprMessageProcessingFailed"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_UPDATE_RECORD_DOES_NOT_EXIST("CprUpdateRecordDoesNotExist"),
  CPR_NEW_RECORD_EXISTS("CprNewRecordExists"),
  CPR_CANDIDATE_RECORD_SEARCH("CprCandidateRecordSearch"),
  CPR_MATCH_PERSON_DUPLICATE("CprMatchPersonRecordDuplicate"),
  CPR_CANDIDATE_RECORD_FOUND_UUID("CprSplinkCandidateRecordsFoundGetUUID")
}
