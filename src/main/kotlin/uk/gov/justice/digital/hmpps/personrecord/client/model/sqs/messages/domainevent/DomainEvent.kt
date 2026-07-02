package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISON_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_DELETED_GDPR
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_RECOVERED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

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
  JsonSubTypes.Type(value = ProbationPersonCreated::class, name = PROBATION_PERSON_CREATED),
  JsonSubTypes.Type(value = ProbationPersonUpdated::class, names = [PROBATION_PERSON_UPDATED, PROBATION_PERSON_RECOVERED, PROBATION_ALIAS_CREATED, PROBATION_ALIAS_UPDATED, PROBATION_ALIAS_DELETED]),
  JsonSubTypes.Type(value = ProbationPersonDeleted::class, names = [PROBATION_PERSON_DELETED, PROBATION_PERSON_DELETED_GDPR]),
  JsonSubTypes.Type(value = ProbationPersonMerged::class, name = PROBATION_PERSON_MERGED),
  JsonSubTypes.Type(value = ProbationPersonUnmerged::class, name = PROBATION_PERSON_UNMERGED),
  JsonSubTypes.Type(value = ProbationAddressCreated::class, name = PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = ProbationAddressUpdated::class, name = PROBATION_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = ProbationAddressDeleted::class, name = PROBATION_ADDRESS_DELETED),
  JsonSubTypes.Type(value = SasAddressUpdated::class, name = SAS_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = SasAddressDeleted::class, name = SAS_ADDRESS_DELETED),
  JsonSubTypes.Type(value = PrisonPersonCreated::class, name = PRISON_PERSON_CREATED),
  JsonSubTypes.Type(value = PrisonPersonUpdated::class, name = PRISON_PERSON_UPDATED),
  JsonSubTypes.Type(value = PrisonPersonMerged::class, name = PRISON_PERSON_MERGED),
  JsonSubTypes.Type(value = CprPersonCreated::class, names = [CPR_PRISON_PERSON_CREATED, CPR_PROBATION_PERSON_CREATED, CPR_COURT_PERSON_CREATED]),
  JsonSubTypes.Type(value = CprAddressCreated::class, name = CPR_PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = CprAddressUpdated::class, name = CPR_PROBATION_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = CprAddressDeleted::class, name = CPR_PROBATION_ADDRESS_DELETED),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface HmppsDomainEvent {
  val eventType: String
}
