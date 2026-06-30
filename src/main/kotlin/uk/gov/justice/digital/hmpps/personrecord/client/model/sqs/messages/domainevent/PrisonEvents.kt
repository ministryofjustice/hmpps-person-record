package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED

data class PrisonPrisonerCreated(
  override val eventType: String = PRISONER_CREATED,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerUpdated(
  override val eventType: String = PRISONER_UPDATED,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMerged(
  override val eventType: String = PRISONER_MERGED,
  val personReference: PersonReference,
  val additionalInformation: PrisonPrisonerMergedInfo,
) : HmppsDomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPrisonerMergedInfo(
  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String,
)
