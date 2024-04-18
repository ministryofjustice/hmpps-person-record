package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.time.LocalDate

class PNCIdentifier(val pncId: String) {

  val valid: Boolean
    get() = pncId.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    return EqualsBuilder.reflectionEquals(this, other)
  }

  override fun hashCode(): Int {
    return HashCodeBuilder.reflectionHashCode(this)
  }

  override fun toString(): String {
    return pncId
  }

  companion object {

    private val PNC_REGEX = Regex("\\d{2,4}(/?)\\d{1,7}[A-Z]\$")

    private const val EMPTY_PNC = ""
    private const val SLASH = "/"
    private const val LONG_PNC_ID_LENGTH = 10
    private const val CENTURY = 100
    private const val YEAR_END = 4

    internal const val SERIAL_NUM_LENGTH = 7

    fun from(inputPncId: String? = ""): PNCIdentifier {
      return when {
        inputPncId.isNullOrEmpty() -> PNCIdentifier(EMPTY_PNC)
        isExpectedFormat(inputPncId) -> toCanonicalForm(inputPncId)
        else -> PNCIdentifier(EMPTY_PNC)
      }
    }

    private fun isExpectedFormat(pnc: String): Boolean = pnc.matches(PNC_REGEX)

    private fun toCanonicalForm(pnc: String): PNCIdentifier {
      val sanitizedPncId = pnc.replace(SLASH, "")
      val canonicalPnc = when {
        isShortFormFormat(sanitizedPncId) -> canonicalShortForm(sanitizedPncId)
        else -> canonicalLongForm(sanitizedPncId)
      }
      return when {
        (canonicalPnc.valid) -> PNCIdentifier(canonicalPnc.value)
        else -> PNCIdentifier(EMPTY_PNC)
      }
    }

    private fun canonicalShortForm(pnc: String): PNC {
      val checkChar = pnc.takeLast(1)
      val year = getYearFromLastTwoDigits(pnc.take(2).toInt()) // E.g. 79 becomes 1979
      val serialNum = pnc.substring(2).dropLast(1) // the non-year id part 123456Z
      return PNC(checkChar, serialNum, year)
    }

    private fun canonicalLongForm(pnc: String): PNC {
      val checkChar = pnc.takeLast(1)
      val year = pnc.take(YEAR_END)
      val serialNum = pnc.dropLast(1).takeLast(SERIAL_NUM_LENGTH)
      return PNC(checkChar, serialNum, year)
    }

    private fun isShortFormFormat(pnc: String): Boolean = pnc.length < LONG_PNC_ID_LENGTH

    private fun isYearThisCentury(year: Int): Boolean {
      return year in 0..currentYearLastTwoDigits()
    }

    private fun isYearLastCentury(year: Int): Boolean {
      return year in currentYearLastTwoDigits() + 1..<CENTURY
    }

    private fun currentYearLastTwoDigits(): Int = LocalDate.now().year % CENTURY

    private fun formatYear(prefix: String, year: Int): String {
      return prefix + year.toString().padStart(2, '0')
    }

    private fun getYearFromLastTwoDigits(year: Int): String {
      return when {
        isYearThisCentury(year) -> formatYear("20", year)
        isYearLastCentury(year) -> formatYear("19", year)
        else -> throw IllegalArgumentException("Could not get year from digits provided")
      }
    }
  }
}

class PNC(private val checkChar: String, serialNum: String, private val yearDigits: String) {

  private val paddedSerialNum: String = padSerialNumber(serialNum)

  val value: String
    get() = "$yearDigits/$paddedSerialNum$checkChar"

  val valid: Boolean
    get() = correctModulus(checkChar.single())

  private fun correctModulus(checkChar: Char): Boolean {
    val modulus = VALID_LETTERS[(yearDigits.takeLast(2) + paddedSerialNum).toInt().mod(VALID_LETTERS.length)]
    return modulus == checkChar
  }

  private fun padSerialNumber(serialNumber: String): String =
    serialNumber.padStart(PNCIdentifier.SERIAL_NUM_LENGTH, '0')

  companion object {
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
  }
}
