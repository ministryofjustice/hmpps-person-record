package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED

data class PrisonPersonCreated(
  override val eventType: String = PRISONER_CREATED,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonUpdated(
  override val eventType: String = PRISONER_UPDATED,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonMerged(
  override val eventType: String = PRISONER_MERGED,
  val personReference: PersonReference,
  val additionalInformation: PrisonPersonMergedInfo,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonMergedInfo(
  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String,
)
