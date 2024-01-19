package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  NEW_CASE_MISSING_PNC("CprNewCaseMissingPnc"),
  NEW_CASE_INVALID_PNC("CprNewCaseInvalidPnc"),
  NEW_CASE_EXACT_MATCH("CprNewCaseExactMatch"),
  NEW_CASE_PARTIAL_MATCH("CprNewCasePartialMatch"),
  NEW_CASE_PERSON_CREATED("CprNewCasePersonCreated"),
  DELIUS_NO_MATCH_FOUND("CprNDeliusNoMatchFound"),
  DELIUS_PARTIAL_MATCH_FOUND("CprNDeliusPartialMatchFound"),
  DELIUS_MATCH_FOUND("CprNDeliusMatchFound"),
  DELIUS_RECORD_CREATION_RECEIVED("CprNewNDeliusRecordCreationReceived"),
  NEW_LIBRA_CASE_RECEIVED("CprLibraHMCTSCaseReceived"),
  NEW_CP_CASE_RECEIVED("CprCommonPlatformHMCTSCaseReceived"),
  UNKNOWN_CASE_RECEIVED("CprUnknownHMCTSCaseReceived"),
  CASE_READ_FAILURE("CprCourtCaseQueueReadFailure"),
  NOMIS_MATCH_FOUND("CprNomisMatchFound"),
  NOMIS_PARTIAL_MATCH_FOUND("CprNomisPartialMatchFound"),
  NOMIS_NO_MATCH_FOUND("CprNomisNoMatchFound"),
  NEW_DELIUS_RECORD_NO_PNC("CprNewnDeliusRecordUnableToMatchNoPnc"),
  NEW_DELIUS_RECORD_PNC_MATCHED("CprNewNDeliusRecordCreatedPNCMatched"),
  NEW_DELIUS_RECORD_NEW_PNC("CprNewNDeliusRecordCreatedNewPNC"),
  DELIUS_OFFENDER_READ_FAILURE("DeliusOffenderReadFailure"),
}
