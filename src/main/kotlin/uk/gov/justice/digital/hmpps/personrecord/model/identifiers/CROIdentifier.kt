package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

class CROIdentifier(inputCroId: String, inputFingerprint: Boolean) {

  val croId: String = inputCroId
  val fingerprint: Boolean = inputFingerprint

  companion object {
    private const val EMPTY_CRO = ""
    private const val SERIAL_NUM_LENGTH = 6

    private val SF_CRO_REGEX = Regex("^SF\\d{2}/\\d{1,6}[A-Z]\$")
    private val CRO_REGEX = Regex("^\\d{1,6}/\\d{2}[A-Z]\$")

    fun from(inputCroId: String? = ""): CROIdentifier {
      if (inputCroId.isNullOrEmpty()) {
        return CROIdentifier(EMPTY_CRO, false)
      }
      return toCanonicalForm(inputCroId)
    }

    private fun toCanonicalForm(inputCroId: String): CROIdentifier {
      return when {
        isSfFormat(inputCroId) -> CROIdentifier(canonicalizeSfFormat(inputCroId), false)
        isExpectedFormat(inputCroId) -> CROIdentifier(inputCroId, true)
        else -> CROIdentifier("", false)
      }
    }

    private fun canonicalizeSfFormat(inputCroId: String): String {
      val checkChar = inputCroId.takeLast(1)
      val strippedCRO = inputCroId.drop(2).split("/") // splits into [YY, NNNNNND]
      val yearDigits = strippedCRO[0]
      val serialNumber = padSerialNumber(strippedCRO[1].dropLast(1))
      return "$serialNumber/$yearDigits$checkChar"
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
