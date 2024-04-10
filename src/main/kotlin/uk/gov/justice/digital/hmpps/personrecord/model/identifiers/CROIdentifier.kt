package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

class CROIdentifier(inputCroId: String, inputFingerprint: Boolean) {

  val croId: String = inputCroId
  val fingerprint: Boolean = inputFingerprint

  companion object {
    private const val EMPTY_CRO = ""
    private const val SF_PREFIX = "SF"

    fun from(inputCroId: String? = ""): CROIdentifier {
      if (inputCroId.isNullOrEmpty()) {
        return CROIdentifier(EMPTY_CRO, false)
      }
      return toCanonicalForm(inputCroId)
    }

    private fun toCanonicalForm(inputCroId: String): CROIdentifier {
      if (isSfFormat(inputCroId)) {
        return CROIdentifier("", false)
      }
      return CROIdentifier("", true)
    }

    private fun isSfFormat(croId: String): Boolean {
      return croId.startsWith(SF_PREFIX)
    }
  }
}
