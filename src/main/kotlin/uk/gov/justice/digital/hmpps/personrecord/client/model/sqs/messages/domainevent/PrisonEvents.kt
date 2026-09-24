package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_UPDATED

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
