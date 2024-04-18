package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import java.time.LocalDate

interface PNCIdentifier {

  val pncId: String
  companion object {

    private const val SLASH = "/"
    private val PNC_REGEX = Regex("\\d{2,4}(/?)\\d{1,7}[A-Z]\$")
    private const val LONG_PNC_ID_LENGTH = 10
    private const val CENTURY = 100
    private const val YEAR_END = 4

    internal const val SERIAL_NUM_LENGTH = 7

    fun from(inputPncId: String? = ""): PNCIdentifier {
      return when {
        inputPncId.isNullOrEmpty() -> MissingPNCIdentifier()
        !isExpectedFormat(inputPncId) -> InvalidPNCIdentifier(inputPncId)
        else -> validOrInvalid(inputPncId)
      }
    }

    private fun isExpectedFormat(pnc: String): Boolean = pnc.matches(PNC_REGEX)

    private fun validOrInvalid(inputPncId: String): PNCIdentifier {
      val pnc = toCanonicalForm(inputPncId)
      return when {
        (pnc.valid) -> ValidPNCIdentifier(pnc.value)
        else -> InvalidPNCIdentifier(inputPncId)
      }
    }

    private fun toCanonicalForm(pnc: String): PNC {
      val sanitizedPncId = pnc.replace(SLASH, "")
      return when {
        isShortFormFormat(sanitizedPncId) -> canonicalShortForm(sanitizedPncId)
        else -> canonicalLongForm(sanitizedPncId)
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

    private fun padSerialNumber(serialNumber: String): String =
      serialNumber.padStart(SERIAL_NUM_LENGTH, '0')
  }
}

class MissingPNCIdentifier : PNCIdentifier {
  override val pncId: String
    get() = ""

  override fun toString(): String {
    return pncId
  }
}

class InvalidPNCIdentifier(private val inputPncId: String) : PNCIdentifier {
  override val pncId: String
    get() = ""

  override fun toString(): String {
    return pncId
  }

  fun invalidValue(): String = inputPncId
}

class ValidPNCIdentifier(private val inputPncId: String) : PNCIdentifier {

  override val pncId: String
    get() = inputPncId

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as PNCIdentifier
    return pncId == other.pncId
  }

  override fun hashCode(): Int {
    return pncId.hashCode()
  }

  override fun toString(): String {
    return pncId
  }
}

class PNC(private val checkChar: String, private val serialNum: String, private val yearDigits: String) {

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