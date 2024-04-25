package uk.gov.justice.digital.hmpps.personrecord.service.type
enum class TelemetryEventType(val eventName: String) {
  DELIUS_MATCH_FOUND("CprNDeliusMatchFound"),
  DELIUS_NO_MATCH_FOUND("CprNDeliusNoMatchFound"),
  DELIUS_PARTIAL_MATCH_FOUND("CprNDeliusPartialMatchFound"),
  DELIUS_RECORD_CREATION_RECEIVED("CprNDeliusNewRecordReceived"),
  HMCTS_EXACT_MATCH("CprHMCTSRecordExactMatchFound"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  HMCTS_PARTIAL_MATCH("CprHMCTSRecordPartialMatchFound"),
  HMCTS_PROCESSING_FAILURE("CprHMCTSMessageProcessingFailed"),
  HMCTS_RECORD_CREATED("CprHMCTSRecordCreated"),
  NEW_DELIUS_RECORD_NEW_PNC("CprNDeliusNewRecordCreated"),
  NEW_DELIUS_RECORD_PNC_MATCHED("CprNewNDeliusNewRecordMatchFound"),
  SPLINK_MATCH_SCORE("CprSplinkMatchProbabilityScore"),
  DELIUS_CALL_FAILED("CprNDeliusCallFailed"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  INVALID_CRO("CprCroInvalid"),
}
