package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class CanonicalNationality(
  val nationality1: String? = "",
  val nationality2: String? = "",

) {
  companion object {

    fun from(personEntity: PersonEntity): CanonicalNationality = CanonicalNationality(
      nationality1 = personEntity.nationality,
    )
  }
}
