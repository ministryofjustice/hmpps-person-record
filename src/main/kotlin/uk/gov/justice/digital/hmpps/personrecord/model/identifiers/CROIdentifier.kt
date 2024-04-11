package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

class CROIdentifier(inputCroId: String, inputFingerprint: Boolean) {

  val croId: String = inputCroId
  val fingerprint: Boolean = inputFingerprint

  val valid: Boolean
    get() = croId.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as CROIdentifier
    return croId == other.croId
  }

  override fun hashCode(): Int {
    return croId.hashCode()
  }

  override fun toString(): String {
    return croId
  }

  companion object {
    private const val EMPTY_CRO = ""
    private const val SLASH = "/"
    private const val LAST_CHARACTER = 9
    private const val SERIAL_NUM_LENGTH = 6
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"

    private val SF_CRO_REGEX = Regex("^SF\\d{2}/\\d{1,6}[A-Z]\$")
    private val CRO_REGEX = Regex("^\\d{1,6}/\\d{2}[A-Z]\$")

    fun from(inputCroId: String? = EMPTY_CRO): CROIdentifier {
      if (inputCroId.isNullOrEmpty()) {
        return CROIdentifier(EMPTY_CRO, false)
      }
      return toCanonicalForm(inputCroId)
    }

    private fun toCanonicalForm(inputCroId: String): CROIdentifier {
      val (canonicalCroId, fingerprint) = when {
        isSfFormat(inputCroId) -> Pair(canonicalizeSfFormat(inputCroId), false)
        isExpectedFormat(inputCroId) -> Pair(canonicalizeStandardFormat(inputCroId), true)
        else -> Pair("", false)
      }
      return when {
        (canonicalCroId.isNotEmpty() && correctModulus(canonicalCroId, fingerprint)) -> CROIdentifier(canonicalCroId, fingerprint)
        else -> CROIdentifier(EMPTY_CRO, false)
      }
    }

    private fun canonicalizeStandardFormat(inputCroId: String): String {
      val checkChar = inputCroId.takeLast(1)
      val (serialNum, yearPart) = inputCroId.split(SLASH) // splits into [NNNNNN, YYD]
      val yearDigits = yearPart.dropLast(1)
      val paddedSerialNum = padSerialNumber(serialNum)
      return "$paddedSerialNum/$yearDigits$checkChar"
    }

    private fun canonicalizeSfFormat(inputCroId: String): String {
      val checkChar = inputCroId.takeLast(1)
      val (yearDigits, serialNum) = inputCroId.drop(2).split(SLASH) // splits into [YY, NNNNNND]
      val paddedSerialNum = padSerialNumber(serialNum.dropLast(1))
      return "$paddedSerialNum/$yearDigits$checkChar"
    }

    private fun correctModulus(inputCroId: String, fingerprint: Boolean): Boolean {
      val checkChar = inputCroId[LAST_CHARACTER]
      val (serialNum, yearDigit) = inputCroId.dropLast(1).split(SLASH) // Append year digit to serial number
      val serialNumToCheck = if (!fingerprint) serialNum.trimStart { it == '0' } else serialNum // Assume if no fingerprint it is SF
      val modulus = VALID_LETTERS[(yearDigit + serialNumToCheck).toLong().mod(VALID_LETTERS.length)]
      return modulus == checkChar
    }

    private fun padSerialNumber(serialNumber: String): String {
      if (serialNumber.length < SERIAL_NUM_LENGTH) {
        return serialNumber.padStart(SERIAL_NUM_LENGTH, '0')
      }
      return serialNumber
    }

    private fun isExpectedFormat(inputCroId: String): Boolean {
      return inputCroId.matches(CRO_REGEX)
    }

    private fun isSfFormat(inputCroId: String): Boolean {
      return inputCroId.matches(SF_CRO_REGEX)
    }
  }
}
