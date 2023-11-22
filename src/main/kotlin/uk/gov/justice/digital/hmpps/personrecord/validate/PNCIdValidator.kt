package uk.gov.justice.digital.hmpps.personrecord.validate

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

const val PNC_REGEX = "\\d{4}([/])\\d{7}[A-Z]{1}\$"

@Component
class PNCIdValidator {

  fun isValid(pncIdentifier: String): Boolean {
    return isValidFormat(pncIdentifier) && hasValidCheckDigit(pncIdentifier)
  }

  private fun hasValidCheckDigit(pncIdentifier: String): Boolean {
    val checkDigit = pncIdentifier.last().uppercaseChar()
    val modulus = calculateModulusOfSerialNumber(pncIdentifier)
    val calculatedCheckDigit = convertNumberToLetterInAlphabet(modulus.toInt())
    return checkDigit == calculatedCheckDigit
  }

  private fun isValidFormat(pncIdentifier: String): Boolean = pncIdentifier.matches(Regex(PNC_REGEX))

  private fun calculateModulusOfSerialNumber(pncIdentifier: String): BigInteger {
    // Starting format is : YYYY/NNNNNNND
    val year = extractTwoDigitYearPart(pncIdentifier) // YY
    val serialNumber = extractSerialNumberFromId(pncIdentifier) // NNNNNNN
    val operand = BigInteger(year + serialNumber) // YYNNNNNNN
    return operand.mod(BigInteger.valueOf(23))
  }

  private fun extractSerialNumberFromId(pncIdentifier: String): String {
    return pncIdentifier.substringAfter('/').substring(0, 7)
  }

  private fun extractTwoDigitYearPart(pncIdentifier: String): String {
    return pncIdentifier.substringBefore('/').takeLast(2)
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
