package uk.gov.justice.digital.hmpps.personrecord.model.types
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class TitleCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  MR("Mr", true, setOf(PROBATION, PRISON)),
  MRS("Mrs", true, setOf(PROBATION, PRISON)),
  MISS("Miss", true, setOf(PROBATION, PRISON)),
  MS("Ms", true, setOf(PROBATION, PRISON)),
  MX("Mx", true, setOf(PROBATION, PRISON)),
  REV("Reverend", true, setOf(PROBATION, PRISON)),
  FR("Father", true, setOf(PROBATION, PRISON)),
  IMAM("Imam", true, setOf(PROBATION, PRISON)),
  RABBI("Rabbi", true, setOf(PROBATION, PRISON)),
  BR("Brother", true, setOf(PROBATION, PRISON)),
  SR("Sister", true, setOf(PROBATION, PRISON)),
  DME("Dame", true, setOf(PROBATION, PRISON)),
  DR("Dr", true, setOf(PROBATION, PRISON)),
  LDY("Lady", true, setOf(PROBATION, PRISON)),
  LRD("Lord", true, setOf(PROBATION, PRISON)),
  SIR("Sir", true, setOf(PROBATION, PRISON)),
  UN("Unknown", true, setOf(PROBATION, PRISON)),
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
