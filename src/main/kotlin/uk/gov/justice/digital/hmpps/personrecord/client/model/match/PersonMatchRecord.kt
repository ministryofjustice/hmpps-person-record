package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class PersonMatchRecord(
  val matchId: String,
  val sourceSystem: String? = "",
  val firstName: String? = "",
  val middleNames: String? = "",
  val lastName: String? = "",
  val dateOfBirth: String? = "",
  val firstNameAliases: List<String> = listOf(),
  val lastNameAliases: List<String> = listOf(),
  val dateOfBirthAliases: List<String> = listOf(),
  val postcodes: List<String> = listOf(),
  val cros: List<String> = listOf(),
  val pncs: List<String> = listOf(),
  val sentenceDates: List<String> = listOf(),
  val crn: String? = "",
  val prisonNumber: String? = "",
  val sourceSystemId: String? = "",
) {

  fun matchingFieldsAreDifferent(personMatchRecord: PersonMatchRecord): Boolean = this != personMatchRecord

  companion object {
    fun from(personEntity: PersonEntity): PersonMatchRecord = PersonMatchRecord(
      matchId = personEntity.matchId.toString(),
      sourceSystem = personEntity.sourceSystem.name,
      firstName = personEntity.getPrimaryName().firstName ?: "",
      middleNames = personEntity.getPrimaryName().middleNames ?: "",
      lastName = personEntity.getPrimaryName().lastName ?: "",
      dateOfBirth = personEntity.getPrimaryName().dateOfBirth?.toString() ?: "",
      firstNameAliases = personEntity.getAliases().mapNotNull { it.firstName }.distinct().sorted(),
      lastNameAliases = personEntity.getAliases().mapNotNull { it.lastName }.distinct().sorted(),
      dateOfBirthAliases = personEntity.getAliases().mapNotNull { it.dateOfBirth }.map { it.toString() }.distinct().sorted(),
      postcodes = personEntity.addresses.mapNotNull { it.postcode }.distinct().sorted(),
      cros = personEntity.references.getType(IdentifierType.CRO).mapNotNull { it.identifierValue }.distinct().sorted(),
      pncs = personEntity.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue }.distinct().sorted(),
      sentenceDates = personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.map { it.toString() }.distinct().sorted(),
      crn = personEntity.crn ?: "",
      prisonNumber = personEntity.prisonNumber ?: "",
      sourceSystemId = personEntity.extractSourceSystemId(),
    )
  }
}
