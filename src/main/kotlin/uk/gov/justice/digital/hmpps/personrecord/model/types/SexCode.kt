package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {

    fun from(libraHearingEvent: LibraHearingEvent?): SexCode? {
      val sexCode = when (libraHearingEvent?.defendantSex) {
        "M" -> M
        "F" -> F
        "NS" -> NS
        null -> null
        else -> N
      }
      return sexCode
    }

    fun from(personDetails: PersonDetails?): SexCode? {
      val sexCode = when (personDetails?.gender) {
        "MALE" -> M
        "FEMALE" -> F
        "NOT SPECIFIED" -> NS
        null -> null
        else -> N
      }
      return sexCode
    }

    fun from(probationCase: ProbationCase?): SexCode? {
      val sexCode = when (probationCase?.gender?.value) {
        "M" -> M
        "F" -> F
        "N" -> N
        null -> null
        else -> NS
      }
      return sexCode
    }
  }
}
