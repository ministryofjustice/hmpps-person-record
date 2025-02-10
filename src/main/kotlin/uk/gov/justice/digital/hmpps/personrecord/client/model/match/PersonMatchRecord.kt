package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class PersonMatchRecord(
  @JsonProperty("matchID")
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
) {
  companion object {
    fun from(personEntity: PersonEntity): PersonMatchRecord = PersonMatchRecord(
      matchId = personEntity.matchId.toString(),
      sourceSystem = personEntity.sourceSystem.name,
      firstName = personEntity.firstName ?: "",
      middleNames = personEntity.middleNames ?: "",
      lastName = personEntity.lastName ?: "",
      dateOfBirth = personEntity.dateOfBirth?.toString() ?: "",
      firstNameAliases = personEntity.pseudonyms.mapNotNull { it.firstName },
      lastNameAliases = personEntity.pseudonyms.mapNotNull { it.lastName },
      dateOfBirthAliases = personEntity.pseudonyms.mapNotNull { it.dateOfBirth }.map { it.toString() },
      postcodes = personEntity.addresses.mapNotNull { it.postcode },
      cros = personEntity.references.getType(IdentifierType.CRO).mapNotNull { it.identifierValue },
      pncs = personEntity.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue },
      sentenceDates = personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.map { it.toString() },
    )
  }
}
