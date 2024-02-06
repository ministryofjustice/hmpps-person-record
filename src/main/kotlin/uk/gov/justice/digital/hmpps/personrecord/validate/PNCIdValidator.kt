package uk.gov.justice.digital.hmpps.personrecord.validate

import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC

private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
private const val SLASH = "/"
private val PNC_REGEX = Regex("\\d{4}(/?)\\d{7}[A-Z]{1}\$")

class PNCIdValidator(private val telemetryService: TelemetryService) {
  fun isValid(pncIdentifier: PNCIdentifier): Boolean {
    val pnc = pncIdentifier.pncId!!
    return (pnc.matches(PNC_REGEX) && correctModulus(pnc))
      .also {
        telemetryService.trackEvent(if (it) VALID_PNC else INVALID_PNC, mapOf("PNC" to pnc, "inputPNC" to pncIdentifier.inputPnc))
      }
  }

  private fun correctModulus(pncIdentifier: String): Boolean {
    val modulusLetter = pncIdentifier[12]
    return VALID_LETTERS[pncIdentifier.replace(SLASH, "").substring(2, 11).toLong().mod(23)] == modulusLetter
  }
}
