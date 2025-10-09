package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

enum class SexualOrientation(val description: String) {
  BIS("Bisexual"),
  HOM("Gay or Lesbian"),
  HET("Heterosexual or Straight"),
  ND("Not Answered"),
  OTH("Other"),
  REF(" Refused"),
  ;

  companion object {

    val probationSexualOrientation: Map<String, SexualOrientation> = mapOf(
      "03" to BIS,
      "02" to HOM,
      "01" to HET,
      "ND" to ND,
      "99" to OTH,
    )

    fun from(probationCase: ProbationCase) = probationCase.sexualOrientation?.value?.let {
      probationSexualOrientation[it]
    }
  }
}
