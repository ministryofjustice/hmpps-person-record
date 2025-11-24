package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank

enum class GenderIdentityCode(val description: String) {
  GIF("Female"),
  GIM("Male"),
  GINB("Non-Binary"),
  GIRF("Prefer not to say"),
  GISD("Prefer to self-describe"),
  UNK("Unknown"),
  ;

  companion object {

    val probationGenderIdentityCode: Map<String, GenderIdentityCode> = mapOf(
      "GIF" to GIF,
      "GIM" to GIM,
      "GINB" to GINB,
      "GIRF" to GIRF,
      "GISD" to GISD,
      "UNK" to UNK,
    )

    fun from(probationCase: ProbationCase?): GenderIdentityCode? = probationCase?.genderIdentity?.value.nullIfBlank()?.let {
      probationGenderIdentityCode.getOrDefault(it, UNK)
    }
  }
}
