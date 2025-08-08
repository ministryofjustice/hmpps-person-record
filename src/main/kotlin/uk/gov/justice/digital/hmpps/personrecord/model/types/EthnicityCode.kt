package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.extentions.nullIfBlank

enum class EthnicityCode {
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
  MERGE,
  NS,
  O1,
  O2,
  O3,
  O9,
  W1,
  W2,
  W3,
  W4,
  W5,
  W9,
  ETH03,
  ETH04,
  ETH05,
  W8,
  Z1,
  P,
  UN,
  ;

  companion object {

    fun from(ethnicity: String?): EthnicityCode? = when (ethnicity?.nullIfBlank()?.uppercase()?.trim()) {
      "A1", "Asian/Asian British : Indian" -> A1
      "A2", "Asian/Asian British : Pakistani" -> A2
      "A3", "Asian/Asian British : Bangladeshi" -> A3
      "A4", "Asian/Asian British : Chinese" -> A4
      "A9", "Asian/Asian British : Any other backgr'nd" -> A9
      "B1", "Black/Black British : Carribean" -> B1
      "B2", "Black/Black British : African" -> B2
      "B9", "Black/Black British: Any other backgr'nd" -> B9
      "M1", "Mixed : White and Black Carribean" -> M1
      "M2", "Mixed : White and Black African" -> M2
      "M3", "Mixed : White and Asian" -> M3
      "M9", "Mixed : Any other background" -> M9
      "MERGE", "Needs to be confirmed following merge" -> MERGE
      "NS", "Prefer not to say" -> NS
      "O2", "Other: Arab" -> O2
      "O3" -> O3
      "O9", "Other: Any other background" -> O9
      "W1", "White : Eng/Welsh/Scot/N.Irish/British" -> W1
      "W2", "White : Irish" -> W2
      "W3", "White: Gypsy or Irish Traveller" -> W3
      "W4" -> W4
      "W5", "White: Roma" -> W5
      "W9", "White : Any other background" -> W9
      "ETH03" -> ETH03
      "ETH04" -> ETH04
      "ETH05" -> ETH05
      "O1", "Chinese" -> O1
      "W8", "White : Irish Traveller/Gypsy".uppercase() -> W8
      "Z1" -> Z1
      "P" -> P
      null -> null
      else -> UN
    }
  }
}
