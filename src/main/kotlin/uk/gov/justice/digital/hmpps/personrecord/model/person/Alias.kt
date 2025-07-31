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
      middleNames = alias.middleName,
      lastName = alias.lastName,
    )

    fun from(alias: ProbationCaseAlias): Alias = Alias(
      firstName = alias.name.firstName,
      middleNames = alias.name.middleNames,
      lastName = alias.name.lastName,
      dateOfBirth = alias.dateOfBirth,
    )

    fun from(alias: PrisonerAlias) = Alias(
      title = alias.title,
      titleCode = TitleCode.from(alias.title),
      firstName = alias.firstName,
      middleNames = alias.middleNames,
      lastName = alias.lastName,
      dateOfBirth = alias.dateOfBirth,
    )

    fun from(pseudonymEntity: PseudonymEntity): Alias = Alias(
      title = pseudonymEntity.title,
      titleCode = TitleCode.from(pseudonymEntity.titleCode?.code),
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      dateOfBirth = pseudonymEntity.dateOfBirth,
    )
  }
}
