package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank

enum class SexCode(val description: String) {
  M("Male"),
  F("Female"),
  N("Not Known / Not Recorded"),
  NS("Not Specified"),
  ;

  companion object {

    val libraSexCode: Map<String, SexCode> = mapOf(
      "M" to M,
      "F" to F,
      "NS" to NS,
    )

    val commonPlatformSexCode: Map<String, SexCode> = mapOf(
      "MALE" to M,
      "FEMALE" to F,
      "NOT SPECIFIED" to NS,
    )

    val probationSexCode: Map<String, SexCode> = mapOf(
      "M" to M,
      "F" to F,
      "N" to N,
    )

    val prisonSexCode: Map<String, SexCode> = mapOf(
      "Male" to M,
      "Female" to F,
      "Not Known / Not Recorded" to N,
      "Not Specified (Indeterminate)" to NS,
    )

    fun from(libraHearingEvent: LibraHearingEvent?): SexCode? = libraHearingEvent?.defendantSex.nullIfBlank()?.let {
      libraSexCode.getOrDefault(it, N)
    }

    fun from(personDetails: PersonDetails?): SexCode? = personDetails?.gender.nullIfBlank()?.let {
      commonPlatformSexCode.getOrDefault(it, N)
    }

    fun from(probationCase: ProbationCase?): SexCode? = probationCase?.gender?.value.nullIfBlank()?.let {
      probationSexCode.getOrDefault(it, NS)
    }

    fun from(prisoner: Prisoner): SexCode? = prisoner.gender.nullIfBlank()?.let {
      prisonSexCode.getOrDefault(it, N)
    }
  }
}
