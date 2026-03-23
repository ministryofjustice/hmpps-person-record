package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.Instant

const val PERSON_UPDATED_EVENT_TYPE = "core-person-record.person.updated"
const val PERSON_CREATED_EVENT_TYPE = "core-person-record.person.created"
const val PERSON_DELETED_EVENT_TYPE = "core-person-record.person.deleted"

data class PersonDomainEvent(
  @JsonProperty("eventType") val eventType: String = PERSON_UPDATED_EVENT_TYPE,
  @JsonProperty("version") val version: Int = 1,
  @JsonProperty("description")
  val description: String? = "Canonical person record has been updated",
  @JsonProperty("detailUrl") val detailUrl: String? = null,
  @JsonProperty("occurredAt") val occurredAt: String,
  @JsonProperty("additionalInformation")
  val additionalInformation: Map<String, String>? = null,
  @JsonProperty("personReference") val personReference: PersonReference? = null,
) {
  companion object {
    fun from(personEntity: PersonEntity, baseUrl: String): PersonDomainEvent {
      val cprUUID = personEntity.personKey?.personUUID?.toString()
      val detailUrl = cprUUID?.let { "$baseUrl/person/$it" }
      val additionalInformation = cprUUID?.let { mapOf("cprUUID" to it) }
      val personReference = buildPersonReference(personEntity)
      return PersonDomainEvent(
        detailUrl = detailUrl,
        occurredAt = Instant.now().toString(),
        additionalInformation = additionalInformation,
        personReference = personReference,
      )
    }

    private fun buildPersonReference(personEntity: PersonEntity): PersonReference {
      val identifiers = mutableListOf<PersonIdentifier>()
      personEntity.crn?.let { identifiers.add(PersonIdentifier("CRN", it)) }
      personEntity.prisonNumber?.let { identifiers.add(PersonIdentifier("NOMS", it)) }
      personEntity.defendantId?.let { identifiers.add(PersonIdentifier("DEFENDANT_ID", it)) }
      personEntity.cId?.let { identifiers.add(PersonIdentifier("C_ID", it)) }
      return PersonReference(identifiers)
    }
  }
}
