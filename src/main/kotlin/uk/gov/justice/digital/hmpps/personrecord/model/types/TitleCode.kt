package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner

enum class TitleCode {
  MR,
  MRS,
  MISS,
  MS,
  MX,
  REV,
  FR,
  IMAM,
  RABBI,
  BR,
  SR,
  DME,
  DR,
  LDY,
  LRD,
  SIR,
  UN,
  ;

  companion object {

    fun from(probationCase: ProbationCase?): TitleCode? = when (probationCase?.title?.value?.trim()) {
      "MR" -> MR
      "MRS" -> MRS
      "MISS" -> MISS
      "MS" -> MS
      "MX" -> MX
      "REV" -> REV
      "DME" -> DME
      "DR" -> DR
      "LDY" -> LDY
      "LRD" -> LRD
      "SIR" -> SIR
      null -> null
      else -> UN
    }

    fun from(prisoner: Prisoner): TitleCode? = when (prisoner.title?.trim()) {
      "Mr" -> MR
      "Mrs" -> MRS
      "Miss" -> MISS
      "Ms" -> MS
      "Reverend" -> REV
      "Father" -> FR
      "Imam" -> IMAM
      "Rabbi" -> RABBI
      "Brother" -> BR
      "Sister" -> SR
      "Dame" -> DME
      "Dr" -> DR
      "Lady" -> LDY
      "Lord" -> LRD
      "Sir" -> SIR
      null -> null
      else -> UN
    }

    fun from(libraHearingEvent: LibraHearingEvent?): TitleCode? = when (libraHearingEvent?.name?.title?.uppercase()?.trim()) {
      "MR" -> MR
      "MRS" -> MRS
      "MISS" -> MISS
      "MS" -> MS
      "MX" -> MX
      "REVEREND" -> REV
      "FATHER" -> FR
      "IMAM" -> IMAM
      "RABBI" -> RABBI
      "Brother" -> BR
      "SISTER" -> SR
      "DME" -> DME
      "DR" -> DR
      "LDY" -> LDY
      "LRD" -> LRD
      "SIR" -> SIR
      null -> null
      else -> UN
    }

    fun from(personDetails: PersonDetails?): TitleCode? = when (personDetails?.title?.trim()) {
      "Mr" -> MR
      "Mrs" -> MRS
      "Miss" -> MISS
      "Ms" -> MS
      "Reverend" -> REV
      "Father" -> FR
      "Imam" -> IMAM
      "Rabbi" -> RABBI
      "Brother" -> BR
      "Sister" -> SR
      "Dame" -> DME
      "Dr" -> DR
      "Lady" -> LDY
      "Lord" -> LRD
      "Sir" -> SIR
      null -> null
      else -> UN
    }
  }
}
