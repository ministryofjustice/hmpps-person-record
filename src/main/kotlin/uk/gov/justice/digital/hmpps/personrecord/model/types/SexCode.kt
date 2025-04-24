package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {

    fun from(personDetails: PersonDetails?): SexCode? {
      val sexCode = when (personDetails?.gender) {
        "MALE" -> M
        "FEMALE" -> F
        "NOT SPECIFIED" -> NS
        else -> N
      }
      return sexCode
    }
  }
}
