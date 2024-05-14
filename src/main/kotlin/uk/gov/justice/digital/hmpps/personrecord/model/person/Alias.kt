package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.DefendantAlias

data class Alias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
) {
  companion object {

    fun from(defendantAlias: DefendantAlias): Alias {
      return Alias(
        firstName = defendantAlias.firstName,
        lastName = defendantAlias.lastName,
        middleNames = defendantAlias.middleName,
      )
    }
  }
}
