package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class CROIdentifier(inputCroId: String, inputFingerprint: Boolean, invalidInputCro: String = EMPTY_CRO) {

  val croId: String = inputCroId
  val fingerprint: Boolean = inputFingerprint

  val valid: Boolean
    get() = croId.isNotEmpty()

  val invalidCro: String = invalidInputCro

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
    private const val SERIAL_NUM_LENGTH = 6

    private val SF_CRO_REGEX = Regex("^SF\\d{2}/\\d{1,6}[A-Z]\$")
    private val CRO_REGEX = Regex("^\\d{1,6}/\\d{2}[A-Z]\$")

    private fun invalidCro(inputCroId: String = ""): CROIdentifier {
      return CROIdentifier(EMPTY_CRO, false, inputCroId)
    }

    fun from(inputCroId: String? = EMPTY_CRO): CROIdentifier {
      return when {
        inputCroId.isNullOrEmpty() -> invalidCro()
        else -> toCanonicalForm(inputCroId)
      }
    }

    private fun toCanonicalForm(inputCroId: String): CROIdentifier {
      val canonicalCro: CRO = when {
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
      val (serialNum, yearPart) = inputCroId.split(SLASH) // splits into [NNNNNN, YYD]
      val yearDigits = yearPart.dropLast(1)
      val paddedSerialNum = padSerialNumber(serialNum)
      return CRO(checkChar, paddedSerialNum, yearDigits)
    }

    private fun canonicalSfFormat(inputCroId: String): CRO {
      val checkChar = inputCroId.takeLast(1)
      val (yearDigits, serialNum) = inputCroId.drop(2).split(SLASH) // splits into [YY, NNNNNND]
      val paddedSerialNum = padSerialNumber(serialNum.dropLast(1))
      return CRO(checkChar, paddedSerialNum, yearDigits, false)
    }

    private fun padSerialNumber(serialNumber: String): String {
      return serialNumber.padStart(SERIAL_NUM_LENGTH, '0')
    }

    private fun isStandardFormat(inputCroId: String): Boolean {
      return inputCroId.matches(CRO_REGEX)
    }

    private fun isSfFormat(inputCroId: String): Boolean {
      return inputCroId.matches(SF_CRO_REGEX)
    }
  }
}

class CRO(private val checkChar: String, private val serialNum: String, private val yearDigits: String, val fingerprint: Boolean = true) {

  val value: String
    get() = "$serialNum/$yearDigits$checkChar"

  val valid: Boolean
    get() = correctModulus(checkChar.single())

  private fun correctModulus(checkChar: Char): Boolean {
    val serialNumToCheck = if (!fingerprint) serialNum.trimStart { it == '0' } else serialNum
    val modulus = VALID_LETTERS[(yearDigits + serialNumToCheck).toLong().mod(VALID_LETTERS.length)]
    return modulus == checkChar
  }
  companion object {
    private const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"
  }
}
