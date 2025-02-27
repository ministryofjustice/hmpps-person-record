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
  val cprUUID: String? = "",
  val firstName: String? = "",
  val middleNames: String? = "",
  val lastName: String? = "",
  val dateOfBirth: String? = "",
  val title: String? = "",
  val masterDefendantId: String? = "",
  val sex: String? = "",
  val religion: String? = "",
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
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      return CanonicalRecord(
        cprUUID = personKey.personId.toString(),
        firstName = latestPerson.firstName,
        middleNames = latestPerson.middleNames,
        lastName = latestPerson.lastName,
        dateOfBirth = latestPerson.dateOfBirth?.toString() ?: "",
        title = latestPerson.title,
        masterDefendantId = latestPerson.masterDefendantId,
        sex = latestPerson.sex,
        religion = latestPerson.religion,
        ethnicity = latestPerson.ethnicity,
        aliases = CanonicalAlias.fromPseudonymEntityList(latestPerson.pseudonyms),
        addresses = CanonicalAddress.fromAddressEntityList(latestPerson.addresses),
        identifiers = getCanonicalIdentifiers(latestPerson) + CanonicalIdentifier.fromReferenceEntityList(latestPerson.references),
        nationalities = CanonicalNationality.from(latestPerson),

      )
    }

    private fun getCanonicalIdentifiers(personEntity: PersonEntity): MutableList<CanonicalIdentifier> = listOf(
      CanonicalIdentifier(CRN, personEntity.crn),
      CanonicalIdentifier(DEFENDANT_ID, personEntity.defendantId),
      CanonicalIdentifier(PRISON_NUMBER, personEntity.prisonNumber),
      CanonicalIdentifier(C_ID, personEntity.cId),
    ).toMutableList()
  }
}
