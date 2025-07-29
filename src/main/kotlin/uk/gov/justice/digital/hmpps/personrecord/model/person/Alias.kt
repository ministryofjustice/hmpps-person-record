package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.DefendantAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerAlias
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import java.time.LocalDate

data class Alias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
  val title: String? = null,
  val titleCode: TitleCode? = null,
  val dateOfBirth: LocalDate? = null,
) {
  companion object {

    fun from(alias: DefendantAlias): Alias = Alias(
      firstName = alias.firstName,
      lastName = alias.lastName,
      middleNames = alias.middleName,
    )

    fun from(alias: ProbationCaseAlias): Alias = Alias(
      firstName = alias.name.firstName,
      lastName = alias.name.lastName,
      middleNames = alias.name.middleNames,
      dateOfBirth = alias.dateOfBirth,
    )

    fun from(alias: PrisonerAlias) = Alias(
      firstName = alias.firstName,
      middleNames = alias.middleNames,
      lastName = alias.lastName,
      dateOfBirth = alias.dateOfBirth,
      title = alias.title,
    )

    fun from(pseudonymEntity: PseudonymEntity): Alias = Alias(
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      dateOfBirth = pseudonymEntity.dateOfBirth,
      title = pseudonymEntity.title,
    )
  }
}
