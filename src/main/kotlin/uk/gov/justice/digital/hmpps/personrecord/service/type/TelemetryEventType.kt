package uk.gov.justice.digital.hmpps.personrecord.service.type
enum class TelemetryEventType(val eventName: String) {
  DELIUS_RECORD_CREATION_RECEIVED("CprNDeliusNewRecordReceived"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  HMCTS_PROCESSING_FAILURE("CprHMCTSMessageProcessingFailed"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_MULTIPLE_RECORDS_FOUND("CprMultipleRecordsFound"),
}
