package uk.gov.justice.digital.hmpps.personrecord.model.identifiers

object IdentifierCheckDigitHandler {
  const val VALID_LETTERS = "ZABCDEFGHJKLMNPQRTUVWXY"

  fun isValid(checkChar: Char, numericPortion: String): Boolean = checkChar == VALID_LETTERS[numericPortion.toInt().mod(VALID_LETTERS.length)]
}

internal fun normalizeIdentifier(input: String?): String? = if (input.isNullOrEmpty()) null else input.uppercase()
