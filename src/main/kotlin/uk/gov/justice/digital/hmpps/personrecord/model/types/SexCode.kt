package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.extentions.nullIfBlank

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {

    val commonPlatformSexCode: Map<String, SexCode> = mapOf(
      "MALE" to M,
      "FEMALE" to F,
      "NOT SPECIFIED" to NS,
    )

    val libraSexCode: Map<String, SexCode> = mapOf(
      "M" to M,
      "F" to F,
      "NS" to NS,
    )

    fun from(libraHearingEvent: LibraHearingEvent?): SexCode? = when (libraHearingEvent?.defendantSex.nullIfBlank()) {
      "M" -> M
      "F" -> F
      "NS" -> NS
      null -> null
      else -> N
    }

    fun from(personDetails: PersonDetails?): SexCode? = when (personDetails?.gender.nullIfBlank()) {
      "MALE" -> M
      "FEMALE" -> F
      "NOT SPECIFIED" -> NS
      null -> null
      else -> N
    }

    fun from(probationCase: ProbationCase?): SexCode? = when (probationCase?.gender?.value.nullIfBlank()) {
      "M" -> M
      "F" -> F
      "N" -> N
      null -> null
      else -> NS
    }

    fun from(prisoner: Prisoner): SexCode? = when (prisoner.gender.nullIfBlank()) {
      "Male" -> M
      "Female" -> F
      "Not Known / Not Recorded" -> N
      "Not Specified (Indeterminate)" -> NS
      null -> null
      else -> N
    }
  }
}
