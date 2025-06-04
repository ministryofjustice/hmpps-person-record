package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {
    fun from(libraHearingEvent: LibraHearingEvent?): SexCode? = libraHearingEvent?.defendantSex?.let {
      mapOf(
        "M" to M,
        "F" to F,
        "NS" to NS,
      ).getOrDefault(it, N)
    }

    fun from(personDetails: PersonDetails?): SexCode? = personDetails?.gender?.let {
      mapOf(
        "MALE" to M,
        "FEMALE" to F,
        "NOT SPECIFIED" to NS,
      ).getOrDefault(it, N)
    }

    fun from(probationCase: ProbationCase?): SexCode? = probationCase?.gender?.value?.let {
      mapOf(
        "M" to M,
        "F" to F,
        "N" to N,
      ).getOrDefault(it, NS)
    }

    fun from(prisoner: Prisoner): SexCode? = when (prisoner.gender) {
      "Male" -> M
      "Female" -> F
      "Not Known / Not Recorded" -> N
      "Not Specified (Indeterminate)" -> NS
      null -> null
      else -> N
    }
  }
}
