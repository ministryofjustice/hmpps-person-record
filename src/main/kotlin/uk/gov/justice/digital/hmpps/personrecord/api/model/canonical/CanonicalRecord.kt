package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.CRN
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.C_ID
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.DEFENDANT_ID
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
  @Schema(description = "Person CPR uuid", example = "f91ef118-a51f-4874-9409-c0538b4ca6fd")
  val cprUUID: String? = "",
  @Schema(description = "Person first name", example = "John")
  val firstName: String? = "",
  @Schema(description = "Person middle names", example = "Morgan")
  val middleNames: String? = "",
  @Schema(description = "Person last name", example = "Doe")
  val lastName: String? = "",
  @Schema(description = "Person date of birth", example = "01/01/1990")
  val dateOfBirth: String? = "",
  @Schema(description = "Person title", example = "Mr")
  val title: String? = "",
  @Schema(description = "Person sex", example = "Male")
  val sex: String? = "",
  @Schema(description = "Person religion", example = "Christian")
  val religion: String? = "",
  @Schema(description = "Person ethnicity", example = "British")
  val ethnicity: String? = "",

  val aliases: List<CanonicalAlias> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of nationality codes",
      example = """
                {"nationalityCode": "UK"},            
            """,
    ),
  )
  var nationalities: List<CanonicalNationality> = emptyList(),
  val addresses: List<CanonicalAddress> = emptyList(),
  val identifiers: List<CanonicalIdentifier> = emptyList(),

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val personList = personKey.personEntities.sortedByDescending { it.lastModified }

      val latestPerson = personList.first()
      return CanonicalRecord(
        cprUUID = personKey.personId.toString(),
        firstName = latestPerson.firstName,
        middleNames = latestPerson.middleNames,
        lastName = latestPerson.lastName,
        dateOfBirth = latestPerson.dateOfBirth?.toString() ?: "",
        title = latestPerson.title,
        sex = latestPerson.sex,
        religion = latestPerson.religion,
        ethnicity = latestPerson.ethnicity,
        aliases = CanonicalAlias.fromPseudonymEntityList(latestPerson.pseudonyms),
        addresses = CanonicalAddress.fromAddressEntityList(latestPerson.addresses),
        identifiers = getCanonicalIdentifiers(personKey.personEntities) + CanonicalIdentifier.fromReferenceEntityList(latestPerson.references),
        nationalities = CanonicalNationality.from(latestPerson),
      )
    }

    private fun getCanonicalIdentifiers(personEntities: List<PersonEntity>): MutableList<CanonicalIdentifier> = listOf(
      CanonicalIdentifier(CRN, personEntities.mapNotNull { it.crn }),
      CanonicalIdentifier(DEFENDANT_ID, personEntities.mapNotNull { it.defendantId }),
      CanonicalIdentifier(PRISON_NUMBER, personEntities.mapNotNull { it.prisonNumber }),
      CanonicalIdentifier(C_ID, personEntities.mapNotNull { it.cId }),
    ).toMutableList()
  }
}
