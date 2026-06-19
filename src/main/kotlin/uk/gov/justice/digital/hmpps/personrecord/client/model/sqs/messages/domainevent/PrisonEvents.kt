package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class PrisonPrisonerCreated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerUpdated(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMerged(
  override val eventType: String,
  override val version: Int = 1,
  override val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: PrisonPrisonerMergedInfo,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMergedInfo(
  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String,
)
