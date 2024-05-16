package uk.gov.justice.digital.hmpps.personrecord.model.person.name

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.DefendantAlias
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.time.LocalDate

data class Name(
  val title: String? = null,
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
  val dateOfBirth: LocalDate? = null,
  val type: NameType,
) {
  companion object {

    fun from(alias: DefendantAlias): Name {
      return Name(
        firstName = alias.firstName,
        lastName = alias.lastName,
        middleNames = alias.middleName,
        type = NameType.ALIAS,
      )
    }

    fun from(alias: ProbationCaseAlias): Name {
      return Name(
        firstName = alias.name.firstName,
        lastName = alias.name.lastName,
        middleNames = alias.name.middleNames,
        type = NameType.ALIAS,
      )
    }

    fun from(alias: Alias): Name =
      Name(
        firstName = alias.firstName,
        lastName = alias.lastName,
        middleNames = alias.middleNames,
        type = NameType.ALIAS,
      )
  }
}
