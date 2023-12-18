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
  NEW_LIBRA_CASE_RECEIVED("CprLibraHMCTSCaseReceived"),
  NEW_CP_CASE_RECEIVED("CprCommonPlatformHMCTSCaseReceived"),
  UNKNOWN_CASE_RECEIVED("CprUnknownHMCTSCase"),
  CASE_READ_FAILURE("CprCourtCaseQueueReadFailure"),
  NOMIS_MATCH_FOUND("CprNomisMatchFound"),
  NOMIS_PARTIAL_MATCH_FOUND("CrpNomisPartialMatchFound"),
}
