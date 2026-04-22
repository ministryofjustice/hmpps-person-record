package uk.gov.justice.digital.hmpps.personrecord.model.types

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
  ;

  companion object {
    fun fromPrison(isPrimary: Boolean, isMail: Boolean): AddressStatusCode? = when {
      isPrimary && isMail -> PM
      isPrimary -> M
      isMail -> MA
      else -> null
    }
  }
}
