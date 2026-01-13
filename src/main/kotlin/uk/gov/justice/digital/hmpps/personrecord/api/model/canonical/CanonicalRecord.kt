package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
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
  @Schema(description = "List of person addresses")
  val addresses: List<CanonicalAddress> = emptyList(),
  @Schema(description = "Person identifiers")
  val identifiers: CanonicalIdentifiers,

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      return CanonicalRecord(
        cprUUID = personKey.personUUID.toString(),
        firstName = latestPerson.getPrimaryName().firstName,
        middleNames = latestPerson.getPrimaryName().middleNames,
        lastName = latestPerson.getPrimaryName().lastName,
        dateOfBirth = latestPerson.getPrimaryName().dateOfBirth?.toString(),
        title = CanonicalTitle.from(latestPerson.getPrimaryName().titleCode),
        sex = CanonicalSex.from(latestPerson.getPrimaryName().sexCode),
        sexualOrientation = CanonicalSexualOrientation.from(latestPerson.sexualOrientation),
        religion = CanonicalReligion.from(latestPerson.religion),
        ethnicity = CanonicalEthnicity.from(latestPerson.ethnicityCode),
        aliases = getAliases(latestPerson),
        addresses = getAddresses(latestPerson),
        identifiers = CanonicalIdentifiers.from(personKey.personEntities),
        nationalities = CanonicalNationality.from(latestPerson),
      )
    }

    fun from(person: PersonEntity): CanonicalRecord = CanonicalRecord(
      firstName = person.getPrimaryName().firstName,
      middleNames = person.getPrimaryName().middleNames,
      lastName = person.getPrimaryName().lastName,
      dateOfBirth = person.getPrimaryName().dateOfBirth?.toString(),
      title = CanonicalTitle.from(person.getPrimaryName().titleCode),
      sex = CanonicalSex.from(person.getPrimaryName().sexCode),
      sexualOrientation = CanonicalSexualOrientation.from(person.sexualOrientation),
      religion = CanonicalReligion.from(person.religion),
      ethnicity = CanonicalEthnicity.from(person.ethnicityCode),
      aliases = getAliases(person),
      addresses = getAddresses(person),
      identifiers = CanonicalIdentifiers.from(person),
      nationalities = CanonicalNationality.from(person),
    )

    private fun getAliases(person: PersonEntity?): List<CanonicalAlias> = CanonicalAlias.from(person) ?: emptyList()

    private fun getAddresses(person: PersonEntity?): List<CanonicalAddress> = person?.addresses?.let { CanonicalAddress.fromAddressEntityList(it) } ?: emptyList()
  }
}
