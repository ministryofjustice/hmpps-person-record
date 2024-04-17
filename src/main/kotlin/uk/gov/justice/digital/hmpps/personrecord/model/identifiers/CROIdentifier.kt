package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier.Companion.SERIAL_NUM_LENGTH
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier.Companion.padSerialNumber

class CROIdentifier(val croId: String, val fingerprint: Boolean, val inputCro: String = EMPTY_CRO) {

  val valid: Boolean
    get() = croId.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    return EqualsBuilder.reflectionEquals(this, other)
  }

  override fun hashCode(): Int {
    return HashCodeBuilder.reflectionHashCode(this)
  }

  override fun toString(): String {
    return croId
  }

  companion object {
    private const val EMPTY_CRO = ""
    private const val SLASH = "/"
    internal const val SERIAL_NUM_LENGTH = 6

    private val SF_CRO_REGEX = Regex("^SF\\d{2}/\\d{1,$SERIAL_NUM_LENGTH}[A-Z]\$")
    private val CRO_REGEX = Regex("^\\d{1,$SERIAL_NUM_LENGTH}/\\d{2}[A-Z]\$")

    private fun invalidCro(inputCroId: String = EMPTY_CRO): CROIdentifier =
      CROIdentifier(EMPTY_CRO, false, inputCroId)

    fun from(inputCroId: String? = EMPTY_CRO): CROIdentifier {
      val canonicalCro: CRO = when {
        inputCroId.isNullOrEmpty() -> return invalidCro()
        isSfFormat(inputCroId) -> canonicalSfFormat(inputCroId)
        isStandardFormat(inputCroId) -> canonicalStandardFormat(inputCroId)
        else -> return invalidCro(inputCroId)
      }
      return when {
        canonicalCro.valid -> CROIdentifier(canonicalCro.value, canonicalCro.fingerprint)
        else -> invalidCro(inputCroId)
      }
    }

    private fun canonicalStandardFormat(inputCroId: String): CRO {
      val checkChar = inputCroId.takeLast(1)
      val (serialNum, yearDigits) = inputCroId.dropLast(1).split(SLASH) // splits into [NNNNNN, YY and drops D]
      return CRO(checkChar, padSerialNumber(serialNum), yearDigits)
    }

    private fun canonicalSfFormat(inputCroId: String): CRO {
      val checkChar = inputCroId.takeLast(1)
      val (yearDigits, serialNum) = inputCroId.drop(2).dropLast(1).split(SLASH) // splits into [YY, NNNNNN and drops D]
      return CRO(checkChar, serialNum, yearDigits, false)
    }

    private fun isStandardFormat(inputCroId: String): Boolean = inputCroId.matches(CRO_REGEX)

    private fun isSfFormat(inputCroId: String): Boolean = inputCroId.matches(SF_CRO_REGEX)

    internal fun padSerialNumber(serialNumber: String): String =
      serialNumber.padStart(SERIAL_NUM_LENGTH, '0')
  }
}

class CRO(private val checkChar: String, private val serialNum: String, private val yearDigits: String, val fingerprint: Boolean = true) {

  val value: String
    get() = "${padSerialNumber(serialNum)}/$yearDigits$checkChar"

  val valid: Boolean
    get() = correctModulus(checkChar.single())

  private fun correctModulus(checkChar: Char): Boolean {
    val modulus = VALID_LETTERS[(yearDigits + serialNum).toInt().mod(VALID_LETTERS.length)]
    return modulus == checkChar
  }

  companion object {
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
  }
}
