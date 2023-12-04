package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  NEW_CASE_MISSING_PNC("CprNewCaseMissingPnc"),
  NEW_CASE_INVALID_PNC("CprNewCaseInvalidPnc"),
  NEW_CASE_EXACT_MATCH("CprNewCaseExactMatch"),
  NEW_CASE_PARTIAL_MATCH("CprNewCasePartialMatch"),
  NEW_CASE_PERSON_CREATED("CprNewCasePersonCreated"),
  NEW_LIBRA_CASE_RECEIVED("CprLibraHMCTSCaseReceived"),
  NEW_CP_CASE_RECEIVED("CprCommonPlatformHMCTSCaseReceived"),
  UNKNOWN_CASE_RECEIVED("CprUnknownHMCTSCase"),
  CASE_READ_FAILURE("CprCourtCaseQueueReadFailure"),
}
