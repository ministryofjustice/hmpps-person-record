package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

enum class TitleCode {
  MR,
  MRS,
  MISS,
  MS,
  MX,
  REV,
  FR,
  IMAM,
  RABBI,
  BR,
  SR,
  DME,
  DR,
  LDY,
  LRD,
  SIR,
  UN,
  ;

  companion object {

    fun from(probationCase: ProbationCase?): TitleCode? = when (probationCase?.title?.value) {
      "MR" -> MR
      "MRS" -> MRS
      "MISS" -> MISS
      "MS" -> MS
      "MX" -> MX
      "REV" -> REV
      "DME" -> DME
      "DR" -> DR
      "LDY" -> LDY
      "LRD" -> LRD
      "SIR" -> SIR
      null -> null
      else -> UN
    }
  }
}
