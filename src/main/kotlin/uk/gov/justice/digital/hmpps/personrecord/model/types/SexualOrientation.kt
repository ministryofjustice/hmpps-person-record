package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

enum class SexualOrientation(val description: String) {
  BIS("Bisexual"),
  HOM("Gay or Lesbian"),
  HET("Heterosexual or Straight"),
  ND("Not Answered"),
  OTH("Other"),
  REF("Refused"),
  UNKNOWN("Unknown"),
  ;

  companion object {

    val probationSexualOrientationMap: Map<String, SexualOrientation> = mapOf(
      "03" to BIS,
      "02" to HOM,
      "01" to HET,
      "ND" to ND,
      "99" to OTH,
    )

    val prisonSexualOrientationMap: Map<String, SexualOrientation> = mapOf(
      "BIS" to BIS,
      "HOM" to HOM,
      "HET" to HET,
      "ND" to ND,
      "OTH" to OTH,
      "REF" to REF,
    )

    fun from(probationCase: ProbationCase) = probationCase.sexualOrientation?.value?.let {
      probationSexualOrientationMap.getOrDefault(it, UNKNOWN)
    }

    fun from(prisonSexualOrientation: PrisonSexualOrientation) = prisonSexualOrientation.sexualOrientationCode?.let {
      prisonSexualOrientationMap.getOrDefault(prisonSexualOrientation.sexualOrientationCode, UNKNOWN)
    }
  }
}
