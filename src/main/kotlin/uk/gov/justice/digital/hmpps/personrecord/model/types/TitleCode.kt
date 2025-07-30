package uk.gov.justice.digital.hmpps.personrecord.model.types

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

    fun from(title: String?): TitleCode? = when (title?.uppercase()?.trim()) {
      "MR" -> MR
      "MRS" -> MRS
      "MISS" -> MISS
      "MS" -> MS
      "MX" -> MX
      "REVEREND", "REV" -> REV
      "FATHER" -> FR
      "IMAM" -> IMAM
      "RABBI" -> RABBI
      "BROTHER" -> BR
      "SISTER" -> SR
      "DAME", "DME" -> DME
      "DR" -> DR
      "LDY", "LADY" -> LDY
      "LRD", "LORD" -> LRD
      "SIR" -> SIR
      null -> null
      else -> UN
    }
  }
}
