package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
  val id: String? = "",
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
  val nationalities: List<CanonicalNationality> = emptyList(),
  val addresses: List<CanonicalAddress> = emptyList(),
  val references: List<CanonicalReference> = emptyList(),
  val identifiers: Identifiers,

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      val additonalIdentifiers = Identifiers.from(personKey)
      return CanonicalRecord(
        id = personKey.personId.toString(),
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
        nationalities = listOf(CanonicalNationality.from(latestPerson)),
        identifiers = additonalIdentifiers,

      )
    }
  }
}
