package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
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
  val references: List<CanonicalReference> = emptyList(),
  val identifiers: CanonicalIdentifiers,

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      val identifiers = CanonicalIdentifiers.from(personKey)
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
        references = CanonicalReference.fromReferenceEntityList(latestPerson.references),
        nationalities = CanonicalNationality.from(latestPerson) ?: emptyList(),
        identifiers = identifiers,

      )
    }
  }
}
