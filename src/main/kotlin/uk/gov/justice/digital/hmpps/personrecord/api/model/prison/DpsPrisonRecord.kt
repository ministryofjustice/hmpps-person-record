package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

data class DpsPrisonRecord(
  @Schema(description = "Person CPR uuid. **If the record has been merged, this will be the CPR uuid of the record it has been merged to**", example = "f91ef118-a51f-4874-9409-c0538b4ca6fd")
  val cprUUID: String? = null,
  @Schema(description = "Person first name", example = "John")
  val firstName: String? = null,
  @Schema(description = "Person middle names", example = "Morgan")
  val middleNames: String? = null,
  @Schema(description = "Person last name", example = "Doe")
  val lastName: String? = null,
  @Schema(description = "Person date of birth", example = "1990-08-21")
  val dateOfBirth: String? = null,
  @Schema(description = "Person disability", example = "true")
  val disability: Boolean? = null,
  @Schema(description = "Person interest to immigration", example = "true")
  val interestToImmigration: Boolean? = null,
  @Schema(description = "Person title")
  val title: CanonicalTitle,
  @Schema(description = "Person sex")
  val sex: CanonicalSex,
  @Schema(description = "Person sexual orientation")
  val sexualOrientation: CanonicalSexualOrientation,
  @Schema(description = "Person religion")
  val religion: CanonicalReligion,
  @Schema(description = "Person ethnicity")
  val ethnicity: CanonicalEthnicity,
  @Schema(description = "List of person aliases")
  val aliases: List<CanonicalAlias> = emptyList(),
  @Schema(description = "List of person nationalities")
  var nationalities: List<CanonicalNationality> = emptyList(),
  // commented out for now as addresses use ZonedDateTime
  //  @Schema(description = "List of person addresses")
  //  val addresses: List<CanonicalAddress> = emptyList(),
  @Schema(description = "Person identifiers")
  val identifiers: CanonicalIdentifiers,
  @Schema(description = "Person religion history")
  val religionHistory: List<PrisonReligion>,
) {
  companion object {
    fun from(personEntity: PersonEntity, prisonReligionEntities: List<PrisonReligionEntity>): DpsPrisonRecord = with(CanonicalRecord.from(personEntity)) {
      DpsPrisonRecord(
        cprUUID = cprUUID,
        firstName = firstName,
        middleNames = middleNames,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        disability = disability,
        interestToImmigration = interestToImmigration,
        title = title,
        sex = sex,
        sexualOrientation = sexualOrientation,
        religion = religion,
        ethnicity = ethnicity,
        aliases = aliases,
        nationalities = nationalities,
        identifiers = identifiers,
        religionHistory = prisonReligionEntities.map { PrisonReligion.from(it) },
      )
    }
  }
}
