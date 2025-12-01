package uk.gov.justice.digital.hmpps.personrecord.model.types
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank

enum class TitleCode(val description: String) {
  MR("Mr"),
  MRS("Mrs"),
  MISS("Miss"),
  MS("Ms"),
  MX("Mx"),
  REV("Reverend"),
  FR("Father"),
  IMAM("Imam"),
  RABBI("Rabbi"),
  BR("Brother"),
  SR("Sister"),
  DME("Dame"),
  DR("Dr"),
  LDY("Lady"),
  LRD("Lord"),
  SIR("Sir"),
  UN("Unknown"),
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
