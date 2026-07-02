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
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_ALIAS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_RECOVERED
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
  JsonSubTypes.Type(value = ProbationOffenderCreated::class, name = NEW_OFFENDER_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderUpdated::class, names = [OFFENDER_PERSONAL_DETAILS_UPDATED, PROBATION_PERSON_RECOVERED, PROBATION_ALIAS_CREATED, PROBATION_ALIAS_UPDATED, PROBATION_ALIAS_DELETED]),
  JsonSubTypes.Type(value = ProbationOffenderDeleted::class, names = [OFFENDER_DELETION, OFFENDER_GDPR_DELETION]),
  JsonSubTypes.Type(value = ProbationOffenderMerged::class, name = OFFENDER_MERGED),
  JsonSubTypes.Type(value = ProbationOffenderUnmerged::class, name = OFFENDER_UNMERGED),
  JsonSubTypes.Type(value = ProbationOffenderAddressCreated::class, name = OFFENDER_ADDRESS_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressUpdated::class, name = OFFENDER_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressDeleted::class, name = OFFENDER_ADDRESS_DELETED),
  JsonSubTypes.Type(value = SasAddressUpdated::class, name = SAS_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = SasAddressDeleted::class, name = SAS_ADDRESS_DELETED),
  JsonSubTypes.Type(value = PrisonPersonCreated::class, name = PRISONER_CREATED),
  JsonSubTypes.Type(value = PrisonPersonUpdated::class, name = PRISONER_UPDATED),
  JsonSubTypes.Type(value = PrisonPersonMerged::class, name = PRISONER_MERGED),
  JsonSubTypes.Type(value = CprPersonCreated::class, names = [CPR_PRISON_PERSON_CREATED, CPR_PROBATION_PERSON_CREATED, CPR_COURT_PERSON_CREATED]),
  JsonSubTypes.Type(value = CprAddressCreated::class, name = CPR_PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = CprAddressUpdated::class, name = CPR_PROBATION_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = CprAddressDeleted::class, name = CPR_PROBATION_ADDRESS_DELETED),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface HmppsDomainEvent {
  val eventType: String
}
