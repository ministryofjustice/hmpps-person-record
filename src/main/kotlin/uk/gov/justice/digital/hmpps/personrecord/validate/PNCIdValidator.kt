package uk.gov.justice.digital.hmpps.personrecord.validate

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC
import java.math.BigInteger

const val PNC_REGEX = "\\d{4}(/?)\\d{7}[A-Z]{1}\$"

class PNCIdValidator(private val telemetryService: TelemetryService) {

  fun isValid(pncIdentifier: PNCIdentifier): Boolean {
    return (pncIdentifier.pncId?.let { isValidFormat(it) } == true &&
      hasValidCheckDigit(pncIdentifier.pncId!!))
        .also {
          telemetryService.trackEvent(if (it) VALID_PNC else INVALID_PNC, mapOf("PNC" to pncIdentifier.pncId))
        }
  }

  private fun hasValidCheckDigit(pncIdentifier: String): Boolean {
    val pncMinusSlash = pncIdentifier.replace(SLASH, "")
    val checkDigit = pncMinusSlash.last().uppercaseChar()
    val calculatedCheckDigit = calculateCheckDigit(pncMinusSlash)
    return checkDigit == calculatedCheckDigit
  }

  private fun isValidFormat(pncIdentifier: String): Boolean = pncIdentifier.matches(Regex(PNC_REGEX))

  private fun calculateModulusOfSerialNumber(pncIdentifier: String): BigInteger {
    // Starting format is : YYYYNNNNNNND
    val year = extractTwoDigitYearPart(pncIdentifier) // YY
    val serialNumber = extractSerialNumberFromId(pncIdentifier, year) // NNNNNNN
    val operand = BigInteger(year + serialNumber) // YYNNNNNNN
    return operand.mod(BigInteger.valueOf(MODULUS_VALUE))
  }

  private fun calculateCheckDigit(pncMinusSlash: String): Char {
    val modulus = calculateModulusOfSerialNumber(pncMinusSlash)
    return convertNumberToLetterInAlphabet(modulus.toInt())
  }

  private fun extractSerialNumberFromId(pncIdentifier: String, year: String): String {
    return pncIdentifier.substringAfter(year).substring(0, 7)
  }

  private fun extractTwoDigitYearPart(pncIdentifier: String): String {
    return pncIdentifier.take(4).takeLast(2)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
    private const val MODULUS_VALUE: Long = 23
    private const val SLASH = "/"

    fun convertNumberToLetterInAlphabet(number: Int): Char {
      // I, O and S are intentionally omitted, and Z is indexed at 0
      return if (number in 0..22) {
        VALID_LETTERS.substring(number, number + 1)[0]
      } else {
        log.warn("Number: $number is out of range")
        throw IllegalArgumentException("Number: $number is out of range")
      }
    }
  }
}
