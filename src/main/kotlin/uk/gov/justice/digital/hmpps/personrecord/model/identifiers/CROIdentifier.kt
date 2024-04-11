package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

class CROIdentifier(inputCroId: String, inputFingerprint: Boolean) {

  val croId: String = inputCroId
  val fingerprint: Boolean = inputFingerprint

  val valid: Boolean
    get() = croId.isNotEmpty()

  companion object {
    private const val EMPTY_CRO = ""
    private const val SLASH = "/"
    private const val LAST_CHARACTER = 9
    private const val SERIAL_NUM_LENGTH = 6
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"

    private val SF_CRO_REGEX = Regex("^SF\\d{2}/\\d{1,6}[A-Z]\$")
    private val CRO_REGEX = Regex("^\\d{1,6}/\\d{2}[A-Z]\$")

    fun from(inputCroId: String? = ""): CROIdentifier {
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
        (canonicalCroId.isNotEmpty() && correctModulus(canonicalCroId)) -> CROIdentifier(canonicalCroId, fingerprint)
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

    private fun correctModulus(inputCroId: String): Boolean {
      val checkChar = inputCroId[LAST_CHARACTER]
      val (serialNum, yearDigit) = inputCroId.dropLast(1).split(SLASH) // Append year digit to serial number
      return VALID_LETTERS[(yearDigit + serialNum).toLong().mod(VALID_LETTERS.length)] == checkChar
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
