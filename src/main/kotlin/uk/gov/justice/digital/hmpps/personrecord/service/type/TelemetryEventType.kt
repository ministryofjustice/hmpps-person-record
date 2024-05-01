package uk.gov.justice.digital.hmpps.personrecord.service.type
enum class TelemetryEventType(val eventName: String) {
  DELIUS_RECORD_CREATION_RECEIVED("CprNDeliusNewRecordReceived"),
  HMCTS_EXACT_MATCH("CprHMCTSRecordExactMatchFound"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  HMCTS_PROCESSING_FAILURE("CprHMCTSMessageProcessingFailed"),
  HMCTS_RECORD_CREATED("CprHMCTSRecordCreated"),
  NEW_DELIUS_RECORD_NEW_PNC("CprNDeliusNewRecordCreated"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  INVALID_CRO("CprCroInvalid"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
}
