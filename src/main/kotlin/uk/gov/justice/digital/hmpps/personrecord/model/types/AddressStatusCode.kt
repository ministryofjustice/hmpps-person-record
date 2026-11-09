package uk.gov.justice.digital.hmpps.personrecord.model.types

import java.util.EnumSet

enum class AddressStatusCode(val description: String) {
  B("Bail"),
  M("Main"),
  MA("Postal"),
  P("Previous"),
  PR("Proposed"),
  PR1("Proposed for Resettlement"),
  RJ("Rejected"),
  RT("ROTL"),
  S("Secondary"),
  PM("Primary and Mail"),
  UNKNOWN("Unknown"),
  ;

  companion object {
    fun fromProbation(value: String): AddressStatusCode = entries.associateBy { it.name }.getOrDefault(value, UNKNOWN)

    fun fromPrison(isPrimary: Boolean, isMail: Boolean): AddressStatusCode? = when {
      isPrimary && isMail -> PM
      isPrimary -> M
      isMail -> MA
      else -> null
    }

    val PRIMARY_TYPES: Set<AddressStatusCode> = EnumSet.of(M, PM)
    val MAIL_TYPES: Set<AddressStatusCode> = EnumSet.of(MA, PM)

    fun AddressStatusCode?.removeFlags(primary: Boolean, mail: Boolean): AddressStatusCode? = if (this in PRIMARY_TYPES || this in MAIL_TYPES) {
      fromPrison(isPrimary = !primary && this in PRIMARY_TYPES, isMail = !mail && this in MAIL_TYPES)
    } else {
      this
    }
  }
}
