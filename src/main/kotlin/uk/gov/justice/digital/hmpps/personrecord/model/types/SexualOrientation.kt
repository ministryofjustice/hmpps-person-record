package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.CPR
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class SexualOrientation(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  BIS("Bisexual", true, setOf(PROBATION, PRISON)),
  HOM("Gay or Lesbian", true, setOf(PROBATION, PRISON)),
  HET("Heterosexual or Straight", true, setOf(PROBATION, PRISON)),
  ND("Not Answered", true, setOf(PROBATION, PRISON)),
  OTH("Other", true, setOf(PROBATION, PRISON)),
  REF("Refused", true, setOf(PROBATION, PRISON)),
  UNKNOWN("Unknown", true, setOf(CPR)),
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

    fun fromProbation(probationCase: ProbationCase) = probationCase.sexualOrientation?.value?.let {
      probationSexualOrientationMap.getOrDefault(it, UNKNOWN)
    }

    fun fromPrison(prisonSexualOrientationCode: String) = prisonSexualOrientationMap.getOrDefault(prisonSexualOrientationCode, UNKNOWN)
  }
}
