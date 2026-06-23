package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION

@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEvent @JsonCreator constructor(
  @JsonProperty("eventType") override val eventType: String,
  @JsonProperty("personReference") val personReference: PersonReference? = null,
  @JsonProperty("additionalInformation") val additionalInformation: AdditionalInformation? = null,
  @JsonProperty("version") val version: Int? = 1,
  @JsonProperty("description") val description: String? = null,
  @JsonProperty("detailUrl") val detailUrl: String? = null,
  @JsonProperty("occurredAt") val occurredAt: String? = null,
) : HmppsDomainEvent
fun DomainEvent.getPrisonNumber() = this.personReference?.identifiers?.first { it.type == "NOMS" }?.value!!
fun DomainEvent.getCrn() = this.personReference?.identifiers?.first { it.type == "CRN" }?.value!!

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "eventType",
  visible = true,
  defaultImpl = DomainEvent::class,
)
@JsonSubTypes(
  JsonSubTypes.Type(value = ProbationOffenderDeleted::class, names = [OFFENDER_DELETION, OFFENDER_GDPR_DELETION]),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface HmppsDomainEvent {
  val eventType: String
}

data class ProbationOffenderDeleted(
  override val eventType: String,
  val personReference: PersonReference,
) : HmppsDomainEvent {
  val crn: String get() = personReference.identifiers?.first { it.type == "CRN" }?.value!!
}
