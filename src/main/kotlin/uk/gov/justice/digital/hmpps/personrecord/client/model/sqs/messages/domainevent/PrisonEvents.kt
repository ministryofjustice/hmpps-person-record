package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_UPDATED

data class PrisonPersonCreated(
  override val eventType: String = PRISON_PERSON_CREATED,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonUpdated(
  override val eventType: String = PRISON_PERSON_UPDATED,
  val personReference: PersonReference,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonMerged(
  override val eventType: String = PRISON_PERSON_MERGED,
  val personReference: PersonReference,
  val additionalInformation: PrisonPersonMergedInfo,
) : DomainEvent {
  val prisonNumber: String get() = personReference.identifiers?.first { it.type == "NOMS" }?.value!!
}

data class PrisonPersonMergedInfo(
  @JsonProperty("removedNomsNumber")
  val sourcePrisonNumber: String,
)
