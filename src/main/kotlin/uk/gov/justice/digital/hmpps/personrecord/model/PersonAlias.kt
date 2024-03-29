package uk.gov.justice.digital.hmpps.personrecord.model

import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.DefendantAlias

data class PersonAlias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleName: String? = null,
) {
  companion object {

    fun from(defendantAlias: DefendantAlias): PersonAlias {
      return PersonAlias(
        firstName = defendantAlias.firstName,
        lastName = defendantAlias.lastName,
        middleName = defendantAlias.middleName,
      )
    }
  }
}
