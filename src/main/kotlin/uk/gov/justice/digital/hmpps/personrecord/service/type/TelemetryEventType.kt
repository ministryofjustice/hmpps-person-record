package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  DOMAIN_EVENT_RECEIVED("CprDomainEventReceived"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  MESSAGE_PROCESSING_FAILED("CprMessageProcessingFailed"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
  CPR_UPDATE_RECORD_DOES_NOT_EXIST("CprUpdateRecordDoesNotExist"),
  CPR_NEW_RECORD_EXISTS("CprNewRecordExists"),
}
