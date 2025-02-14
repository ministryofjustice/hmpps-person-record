package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonMatchIdentifier(
  val matchId: String,
) {
  companion object {
    fun from(personEntity: PersonEntity): PersonMatchIdentifier = PersonMatchIdentifier(matchId = personEntity.matchId.toString())
  }
}
