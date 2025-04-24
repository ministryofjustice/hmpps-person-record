package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {

    fun from(defendant: Defendant): SexCode? {
      val sexCode = when (defendant.personDefendant?.personDetails?.gender) {
        "MALE" -> M
        else -> null
      }
      return sexCode
    }
  }
}
