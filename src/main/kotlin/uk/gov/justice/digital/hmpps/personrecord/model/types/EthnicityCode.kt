package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class EthnicityCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  A1("Asian/Asian British : Indian", true, setOf(PROBATION, PRISON)),
  A2("Asian/Asian British : Pakistani", true, setOf(PROBATION, PRISON)),
  A3("Asian/Asian British : Bangladeshi", true, setOf(PROBATION, PRISON)),
  A4("Asian/Asian British : Chinese", true, setOf(PROBATION, PRISON)),
  A9("Asian/Asian British : Any other backgr'nd", true, setOf(PROBATION, PRISON)),
  B1("Black/Black British : Caribbean", true, setOf(PROBATION, PRISON)),
  B2("Black/Black British : African", true, setOf(PROBATION, PRISON)),
  B9("Black/Black British : Any other backgr'nd", true, setOf(PROBATION, PRISON)),
  ETH03("Other (historic)", true, setOf(PROBATION, PRISON)),
  M1("Mixed : White and Black Caribbean", true, setOf(PROBATION, PRISON)),
  M2("Mixed : White and Black African", true, setOf(PROBATION, PRISON)),
  M3("Mixed : White and Asian", true, setOf(PROBATION, PRISON)),
  M9("Mixed : Any other background", true, setOf(PROBATION, PRISON)),
  MERGE("Needs to be confirmed following merge", true, setOf(PROBATION, PRISON)),
  NS("Prefer not to say", true, setOf(PROBATION, PRISON)),
  O1("Chinese", true, setOf(PROBATION, PRISON)),
  O2("Other : Arab", true, setOf(PROBATION, PRISON)),
  O9("Other : Any other background", true, setOf(PROBATION, PRISON)),
  UN("Unknown", true, setOf(PROBATION, PRISON)),
  W1("White : Eng/Welsh/Scot/N.Irish/British", true, setOf(PROBATION, PRISON)),
  W2("White : Irish", true, setOf(PROBATION, PRISON)),
  W3("White : Gypsy or Irish Traveller", true, setOf(PROBATION, PRISON)),
  W4("White : Gypsy or Irish Traveller", true, setOf(PROBATION, PRISON)),
  W5("White : Roma", true, setOf(PROBATION, PRISON)),
  W8("White : Irish Traveller/Gypsy", true, setOf(PROBATION, PRISON)),
  W9("White : Any other background", true, setOf(PROBATION, PRISON)),
  Z1("Missing (IAPS)", true, setOf(PROBATION, PRISON)),
  ;

  companion object {

    val probationEthnicity = listOf(
      A1,
      A2,
      A3,
      A4,
      A9,
      B1,
      B2,
      B9,
      M1,
      M2,
      M3,
      M9,
      NS,
      O2,
      O9,
      W1,
      W2,
      W3,
      W4,
      W5,
      W9,
      ETH03,
      O1,
      Z1,
    ).associateBy { it.name }

    val commonPlatformEthnicity = listOf(
      A1,
      A2,
      A3,
      A4,
      A9,
      B1,
      B2,
      B9,
      M1,
      M2,
      M3,
      M9,
      NS,
      O2,
      O9,
      W1,
      W2,
      W3,
      W9,
      O1,
    ).associateBy { it.name }

    val prisonEthnicity: Map<String, EthnicityCode> = mapOf(
      "Asian/Asian British: Indian" to A1,
      "Asian/Asian British: Pakistani" to A2,
      "Asian/Asian British: Bangladeshi" to A3,
      "Asian/Asian British: Chinese" to A4,
      "Asian/Asian British: Any other backgr'nd" to A9,
      "Black/Black British: Caribbean" to B1,
      "Black/Black British: African" to B2,
      "Black/Black British: Any other backgr'nd" to B9,
      "Mixed: White and Black Caribbean" to M1,
      "Mixed: White and Black African" to M2,
      "Mixed: White and Asian" to M3,
      "Mixed: Any other background" to M9,
      "Needs to be confirmed following merge" to MERGE,
      "Prefer not to say" to NS,
      "Other: Arab" to O2,
      "Other: Any other background" to O9,
      "White: Eng./Welsh/Scot./N.Irish/British" to W1,
      "White: Irish" to W2,
      "White: Gypsy or Irish Traveller" to W3,
      "White: Roma" to W5,
      "White: Any other background" to W9,
      "Chinese" to O1,
      "White : Irish Traveller/Gypsy" to W8,
    )

    fun fromProbation(code: String?): EthnicityCode? = code?.let { probationEthnicity.getOrDefault(code, UN) }

    fun fromCommonPlatform(code: String?): EthnicityCode? = code?.let { commonPlatformEthnicity.getOrDefault(code, UN) }

    fun fromPrison(description: String?): EthnicityCode? = description?.let { prisonEthnicity.getOrDefault(it, UN) }
  }
}
