package uk.gov.justice.digital.hmpps.personrecord.service.type

enum class TelemetryEventType(val eventName: String) {
  NEW_CASE_MISSING_PNC("CprNewCaseMissingPnc"),
  NEW_CASE_INVALID_PNC("CprNewCaseInvalidPnc"),
  NEW_CASE_EXACT_MATCH("CprNewCaseExactMatch"),
  NEW_CASE_PARTIAL_MATCH("CprNewCasePartialMatch"),
}
