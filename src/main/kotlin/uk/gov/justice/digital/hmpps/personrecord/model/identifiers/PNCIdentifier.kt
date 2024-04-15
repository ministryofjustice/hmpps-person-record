package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import java.time.LocalDate

interface PNCIdentifier {

  val pncId: String
  companion object {
    fun from(inputPncId: String? = ""): PNCIdentifier {
      if (inputPncId.isNullOrEmpty()) {
        return MissingPNCIdentifier()
      }
      return validOrInvalid(inputPncId)
    }

    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
    private const val SLASH = "/"
    private val PNC_REGEX = Regex("\\d{4}(/?)\\d{7}[A-Z]\$")
    private const val LONG_PNC_ID_LENGTH = 10
    private const val CENTURY = 100
    private const val LAST_CHARACTER = 12
    private const val YEAR_END = 4

    private fun validOrInvalid(inputPncId: String): PNCIdentifier {
      val pncId = toCanonicalForm(inputPncId)
      return when {
        (pncId.matches(PNC_REGEX) && correctModulus(pncId)) -> ValidPNCIdentifier(pncId)
        else -> InvalidPNCIdentifier(inputPncId)
      }
    }
    private fun correctModulus(pncId: String): Boolean {
      val modulusLetter = pncId[LAST_CHARACTER]
      return VALID_LETTERS[pncId.replace(SLASH, "").substring(2, LAST_CHARACTER - 1).toLong().mod(VALID_LETTERS.length)] == modulusLetter
    }
    private fun toCanonicalForm(pnc: String): String {
      val sanitizedPncId = pnc.replace("/", "")

      if (sanitizedPncId.length < LONG_PNC_ID_LENGTH) { // this is a short form PNC e.g. 79/123456Z
        val yearDigits = sanitizedPncId.take(2).also { digits ->
          if (!digits.all { character -> character.isDigit() }) {
            return pnc
          }
        }

        val year = getYearFromLastTwoDigits(yearDigits.toInt()) // E.g. 79 becomes 1979
        val remainingIdChars = sanitizedPncId.substring(2) // the non-year id part 123456Z
        // pad out with zeros: 123456Z becomes 0123456Z
        val standardizedId = remainingIdChars.padStart(LONG_PNC_ID_LENGTH - 2, '0')
        return "$year/$standardizedId" // 1979/0123456Z
      }
      return withForwardSlash(pnc)
    }

    private fun withForwardSlash(pnc: String): String {
      return when {
        !pnc.contains("/") -> {
          pnc.substring(0, YEAR_END) + "/" + pnc.substring(YEAR_END)
        }
        else -> pnc
      }
    }

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
