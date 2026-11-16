package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerAlias
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class SexCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  M("Male", true, setOf(PROBATION, PRISON)),
  F("Female", true, setOf(PROBATION, PRISON)),
  N("Not Known / Not Recorded", true, setOf(PROBATION, PRISON)),
  NS("Not Specified", true, setOf(PROBATION, PRISON)),
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

    fun from(probationCaseAlias: ProbationCaseAlias?): SexCode? = probationCaseAlias?.gender?.value.nullIfBlank()?.let {
      probationSexCode.getOrDefault(it, NS)
    }

    fun from(prisoner: Prisoner): SexCode? = prisoner.gender.nullIfBlank()?.let {
      prisonSexCode.getOrDefault(it, N)
    }

    fun from(prisonerAlias: PrisonerAlias): SexCode? = prisonerAlias.gender.nullIfBlank()?.let {
      prisonSexCode.getOrDefault(it, N)
    }
  }
}
