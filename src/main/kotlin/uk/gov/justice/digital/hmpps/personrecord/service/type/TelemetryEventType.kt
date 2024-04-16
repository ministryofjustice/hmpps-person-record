package uk.gov.justice.digital.hmpps.personrecord.service.type
enum class TelemetryEventType(val eventName: String) {
  DELIUS_MATCH_FOUND("CprNDeliusMatchFound"),
  DELIUS_NO_MATCH_FOUND("CprNDeliusNoMatchFound"),
  DELIUS_PARTIAL_MATCH_FOUND("CprNDeliusPartialMatchFound"),
  DELIUS_PNC_MISMATCH("CprNDeliusPncNoMatchFound"),
  DELIUS_RECORD_CREATION_RECEIVED("CprNDeliusNewRecordReceived"),
  HMCTS_EXACT_MATCH("CprHMCTSRecordExactMatchFound"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  HMCTS_PARTIAL_MATCH("CprHMCTSRecordPartialMatchFound"),
  HMCTS_PROCESSING_FAILURE("CprHMCTSMessageProcessingFailed"),
  HMCTS_RECORD_CREATED("CprHMCTSRecordCreated"),
  NEW_DELIUS_RECORD_NEW_PNC("CprNDeliusNewRecordCreated"),
  NEW_DELIUS_RECORD_PNC_MATCHED("CprNewNDeliusNewRecordMatchFound"),
  NOMIS_MATCH_FOUND("CprNomisMatchFound"),
  NOMIS_NO_MATCH_FOUND("CprNomisNoMatchFound"),
  NOMIS_PARTIAL_MATCH_FOUND("CprNomisPartialMatchFound"),
  NOMIS_PNC_MISMATCH("CprNomisPncNoMatchFound"),
  SPLINK_MATCH_SCORE("CprSplinkMatchProbabilityScore"),
  NOMIS_PRISONER_DETAILS_FOUND("CprNomisPrisonerDetailsFound"),
  NOMIS_PRISONER_DETAILS_NOT_FOUND("CprNomisErrorNoPrisonerDetailsFound"),
  DELIUS_CALL_FAILED("CprNDeliusCallFailed"),
  NOMIS_CALL_FAILED("CprNomisCallFailed"),
  INVALID_CRO("CprCroInvalid"),
}
