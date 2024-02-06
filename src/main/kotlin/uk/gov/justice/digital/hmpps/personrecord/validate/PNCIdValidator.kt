package uk.gov.justice.digital.hmpps.personrecord.validate

import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC

const val PNC_REGEX = "\\d{4}(/?)\\d{7}[A-Z]{1}\$"

class PNCIdValidator(private val telemetryService: TelemetryService) {
  fun isValid(pncId: PNCIdentifier): Boolean {
    val pncIdentifier = pncId.pncId!!
    return (pncIdentifier.matches(Regex(PNC_REGEX)) && correctModulus(pncIdentifier))
      .also {
        telemetryService.trackEvent(if (it) VALID_PNC else INVALID_PNC, mapOf("PNC" to pncIdentifier, "inputPNC" to pncId.inputPnc))
      }
  }

  private fun correctModulus(pncIdentifier: String): Boolean {
    val modulusLetter = pncIdentifier[12]
    return VALID_LETTERS[pncIdentifier.replace(SLASH, "").substring(2, 11).toLong().mod(23)] == modulusLetter
  }

  companion object {
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
    private const val SLASH = "/"
  }
}
