package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

const val PRISON_PERSON_CREATED = "prisoner-offender-search.prisoner.created"
const val PRISON_PERSON_UPDATED = "prisoner-offender-search.prisoner.updated"

data class PrisonPersonCreated(
  override val eventType: String = PRISON_PERSON_CREATED,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.getPrisonNumber()
}

data class PrisonPersonUpdated(
  override val eventType: String = PRISON_PERSON_UPDATED,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.getPrisonNumber()
}

private fun PersonReference.getPrisonNumber() = this.identifiers?.first { it.type == "NOMS" }?.value!!
