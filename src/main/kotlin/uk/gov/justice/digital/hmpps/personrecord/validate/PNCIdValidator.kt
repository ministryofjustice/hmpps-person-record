package uk.gov.justice.digital.hmpps.personrecord.validate

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.math.BigInteger

const val PNC_REGEX = "\\d{4}(/?)\\d{7}[A-Z]{1}\$"

class PNCIdValidator(private val telemetryService: TelemetryService) {

  fun isValid(pncIdentifier: String?): Boolean {
    if (pncIdentifier.isNullOrEmpty()) {
      telemetryService.trackEvent(TelemetryEventType.MISSING_PNC, emptyMap())
      return false
    }
    val result = isValidFormat(pncIdentifier) && hasValidCheckDigit(pncIdentifier)
    return result.also {
      if (!it) {
        telemetryService.trackEvent(TelemetryEventType.INVALID_PNC, mapOf("PNC" to pncIdentifier))
      }
    }
  }

  private fun hasValidCheckDigit(pncIdentifier: String): Boolean {
    val pncMinusSlash = pncIdentifier.replace("/", "")
    val checkDigit = pncMinusSlash.last().uppercaseChar()
    val modulus = calculateModulusOfSerialNumber(pncMinusSlash)
    val calculatedCheckDigit = convertNumberToLetterInAlphabet(modulus.toInt())
    return checkDigit == calculatedCheckDigit
  }

  private fun isValidFormat(pncIdentifier: String): Boolean = pncIdentifier.matches(Regex(PNC_REGEX))

  private fun calculateModulusOfSerialNumber(pncIdentifier: String): BigInteger {
    // Starting format is : YYYYNNNNNNND
    val year = extractTwoDigitYearPart(pncIdentifier) // YY
    val serialNumber = extractSerialNumberFromId(pncIdentifier, year) // NNNNNNN
    val operand = BigInteger(year + serialNumber) // YYNNNNNNN
    return operand.mod(BigInteger.valueOf(23))
  }

  private fun extractSerialNumberFromId(pncIdentifier: String, year: String): String {
    return pncIdentifier.substringAfter(year).substring(0, 7)
  }

  private fun extractTwoDigitYearPart(pncIdentifier: String): String {
    return pncIdentifier.take(4).takeLast(2)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun convertNumberToLetterInAlphabet(number: Int): Char {
      // I, O and S are intentionally omitted, and Z is indexed at 0
      val lettersInAlphabet = "ZABCDEFGHJKLMNPQRTUVWXY"
      return if (number in 0..22) {
        lettersInAlphabet.substring(number, number + 1)[0]
      } else {
        log.warn("Number: $number is out of range")
        throw IllegalArgumentException("Number: $number is out of range")
      }
    }
  }
}
