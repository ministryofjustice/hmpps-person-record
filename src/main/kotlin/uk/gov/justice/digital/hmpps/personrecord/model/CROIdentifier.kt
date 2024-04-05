package uk.gov.justice.digital.hmpps.personrecord.model

interface CROIdentifier {

  val croId: String

  companion object {
    fun from(inputCroId: String? = ""): CROIdentifier {
      if (inputCroId.isNullOrEmpty()) {
        return MissingCROIdentifier()
      }
      return validOrInvalid(inputCroId)
    }

    private fun validOrInvalid(inputCroId: String): CROIdentifier {
      return ValidCROIdentifier("Placeholder")
    }
  }
}

class ValidCROIdentifier(private val inputCroId: String) : CROIdentifier {

  override val croId: String
    get() = inputCroId

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
}

class MissingCROIdentifier : CROIdentifier {
  override val croId: String
    get() = ""

  override fun toString(): String {
    return croId
  }
}

class InvalidCROIdentifier(private val inputPncId: String) : CROIdentifier {
  override val croId: String
    get() = ""

  override fun toString(): String {
    return croId
  }
}
