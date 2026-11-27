package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SyncAddressesHandler.Companion.toPrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SyncContactsHandler.Companion.toDto
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

data class SyncReconciliation(
  @Schema(description = "The prison number", example = "A1234AA")
  val prisonNumber: String,
  @Schema(description = "The primary name and aliases, each with their associated identifiers")
  val aliases: List<PrisonAlias>,
  @Schema(description = "The addresses, each with their address usages and address contacts")
  val addresses: List<PrisonAddress>,
  @Schema(description = "The contacts owned by the person rather than an address")
  val contacts: List<PrisonContact>,
  @Schema(description = "The prison religion history")
  val religions: List<PrisonReligion>,
) {
  companion object {
    fun from(
      personEntity: PersonEntity,
      prisonReligionEntities: List<PrisonReligionEntity>,
      prisonPseudonymEntities: List<PrisonPseudonymEntity>,
    ): SyncReconciliation = SyncReconciliation(
      prisonNumber = personEntity.prisonNumber!!,
      aliases = personEntity.pseudonyms
        .associateWith { pseudonym -> prisonPseudonymEntities.firstOrNull { it.id == pseudonym.id } }
        .map { (pseudonym, prisonPseudonym) -> pseudonym.toPrisonAlias(prisonPseudonym) },
      addresses = personEntity.addresses.map { it.toPrisonAddress() },
      contacts = personEntity.contacts.map { it.toDto() },
      religions = prisonReligionEntities.map { PrisonReligion.from(it) },
    )

    private fun PseudonymEntity.toPrisonAlias(prisonPseudonymEntity: PrisonPseudonymEntity?) = PrisonAlias(
      cprAliasId = updateId?.toString(),
      titleCode = titleCode,
      firstName = firstName,
      middleNames = middleNames,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      sexCode = sexCode,
      isPrimary = nameType == NameType.PRIMARY,
      birthPlace = birthplace,
      birthCountry = birthCountryCode,
      ethnicity = ethnicityCode,
      identifiers = references.map { it.toPrisonIdentifier() },
      suffix = prisonPseudonymEntity?.suffix,
      nameType = prisonPseudonymEntity?.nameType,
      createDateTime = createDateTime,
      createUserId = createUserId,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
    )

    private fun ReferenceEntity.toPrisonIdentifier() = PrisonIdentifier(
      cprIdentifierId = updateId?.toString(),
      type = identifierType,
      value = identifierValue.orEmpty(),
      comment = comment,
      verified = isVerified ?: false,
      createDateTime = createDateTime,
      createUserId = createUserId,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
    )
  }
}
