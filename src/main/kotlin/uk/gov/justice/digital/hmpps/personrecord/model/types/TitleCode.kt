package uk.gov.justice.digital.hmpps.personrecord.model.types
import uk.gov.justice.digital.hmpps.personrecord.extentions.nullIfBlank

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
    val titleCodeMap: Map<String, TitleCode> = mapOf(
      "MR" to MR,
      "MRS" to MRS,
      "MISS" to MISS,
      "MS" to MS,
      "MX" to MX,
      "REVEREND" to REV,
      "REV" to REV,
      "FATHER" to FR,
      "IMAM" to IMAM,
      "RABBI" to RABBI,
      "BROTHER" to BR,
      "SISTER" to SR,
      "DAME" to DME,
      "DME" to DME,
      "DR" to DR,
      "LDY" to LDY,
      "LADY" to LDY,
      "LRD" to LRD,
      "LORD" to LRD,
      "SIR" to SIR,
    )

    fun from(title: String?): TitleCode? = title?.nullIfBlank()?.let { titleCodeMap.getOrDefault(it.uppercase().trim(), UN) }
  }
}
