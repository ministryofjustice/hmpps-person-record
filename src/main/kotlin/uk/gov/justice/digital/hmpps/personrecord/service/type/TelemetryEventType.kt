package uk.gov.justice.digital.hmpps.personrecord.service.type
enum class TelemetryEventType(val eventName: String) {
  DOMAIN_EVENT_RECEIVED("CprDomainEventReceived"),
  HMCTS_MESSAGE_RECEIVED("CprHMCTSMessageReceived"),
  HMCTS_PROCESSING_FAILURE("CprHMCTSMessageProcessingFailed"),
  MATCH_CALL_FAILED("CprMatchCallFailed"),
  NOMIS_MESSAGE_RECEIVED("CprNOMISMessageReceived"),
  CPR_RECORD_CREATED("CprRecordCreated"),
  CPR_RECORD_UPDATED("CprRecordUpdated"),
}
