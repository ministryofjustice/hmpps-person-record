package uk.gov.justice.digital.hmpps.personrecord.validate

import java.time.LocalDate

const val LONG_PNC_ID_LENGTH = 10

interface PNCIdentifier {

  val pncId: String
  companion object {
    fun from(inputPncId: String? = ""): PNCIdentifier {
      if (inputPncId.isNullOrEmpty()) {
        return MissingPNCIdentifier()
      }
      val pncId = toCanonicalForm(inputPncId)
      if (isValid(pncId)) {
        return ValidPNCIdentifier(pncId)
      }
      return InvalidPNCIdentifier()
    }

    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
    private const val SLASH = "/"
    private val PNC_REGEX = Regex("\\d{4}(/?)\\d{7}[A-Z]\$")
    fun isValid(pncId: String): Boolean {
      return (pncId.matches(PNC_REGEX) && correctModulus(pncId))
    }
    private fun correctModulus(pncId: String): Boolean {
      val modulusLetter = pncId[12]
      return VALID_LETTERS[pncId.replace(SLASH, "").substring(2, 11).toLong().mod(23)] == modulusLetter
    }
    private fun toCanonicalForm(pnc: String): String {
      return when {
        pnc.isBlank() -> pnc
        else -> {
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
      }
    }

    private fun withForwardSlash(pnc: String): String {
      return when {
        !pnc.contains("/") -> {
          pnc.substring(0, 4) + "/" + pnc.substring(4)
        }
        else -> pnc
      }
    }

    private fun isYearThisCentury(year: Int): Boolean {
      val currentYearLastTwoDigits = LocalDate.now().year % 100
      return year in 0..currentYearLastTwoDigits
    }

    private fun isYearLastCentury(year: Int): Boolean {
      val currentYearLastTwoDigits = LocalDate.now().year % 100
      return year in currentYearLastTwoDigits + 1..99
    }

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

class InvalidPNCIdentifier : PNCIdentifier {
  override val pncId: String
    get() = ""

  override fun toString(): String {
    return pncId
  }
}

class ValidPNCIdentifier(val inputPncId: String) : PNCIdentifier {

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
