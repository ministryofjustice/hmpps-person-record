package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "eventType",
  visible = true,
)
@JsonSubTypes(
  JsonSubTypes.Type(value = ProbationPersonCreated::class, name = PROBATION_PERSON_CREATED),
  JsonSubTypes.Type(value = ProbationPersonUpdated::class, names = [PROBATION_PERSON_UPDATED, PROBATION_ALIAS_CREATED, PROBATION_ALIAS_UPDATED, PROBATION_ALIAS_DELETED, PROBATION_REFERENCE_CREATED, PROBATION_REFERENCE_DELETED, PROBATION_REFERENCE_RECOVERED]),
  JsonSubTypes.Type(value = ProbationPersonDeleted::class, names = [PROBATION_PERSON_DELETED, PROBATION_PERSON_DELETED_GDPR]),
  JsonSubTypes.Type(value = ProbationPersonRecovered::class, name = PROBATION_PERSON_RECOVERED),
  JsonSubTypes.Type(value = ProbationPersonMerged::class, name = PROBATION_PERSON_MERGED),
  JsonSubTypes.Type(value = ProbationPersonUnmerged::class, name = PROBATION_PERSON_UNMERGED),
  JsonSubTypes.Type(value = ProbationAddressCreated::class, name = PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = ProbationAddressUpdated::class, name = PROBATION_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = ProbationAddressDeleted::class, name = PROBATION_ADDRESS_DELETED),
  JsonSubTypes.Type(value = SasAddressUpdated::class, name = SAS_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = SasAddressDeleted::class, name = SAS_ADDRESS_DELETED),
  JsonSubTypes.Type(value = SasAddressArrived::class, name = SAS_ADDRESS_ARRIVED),
  JsonSubTypes.Type(value = PrisonPersonCreated::class, name = PRISON_PERSON_CREATED),
  JsonSubTypes.Type(value = PrisonPersonUpdated::class, name = PRISON_PERSON_UPDATED),
  JsonSubTypes.Type(value = CprPersonCreated::class, names = [CPR_PRISON_PERSON_CREATED, CPR_PROBATION_PERSON_CREATED]),
  JsonSubTypes.Type(value = CprPersonUpdated::class, name = CPR_PROBATION_PERSON_UPDATED),
  JsonSubTypes.Type(value = CprPersonDeleted::class, name = CPR_PROBATION_PERSON_DELETED),
  JsonSubTypes.Type(value = CprPersonMerged::class, names = [CPR_PRISON_PERSON_MERGED, CPR_PROBATION_PERSON_MERGED]),
  JsonSubTypes.Type(value = CprPersonUnmerged::class, name = CPR_PROBATION_PERSON_UNMERGED),
  JsonSubTypes.Type(value = CprAddressCreated::class, name = CPR_PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = CprAddressUpdated::class, name = CPR_PROBATION_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = CprAddressDeleted::class, name = CPR_PROBATION_ADDRESS_DELETED),
  JsonSubTypes.Type(value = CprReligionCreated::class, name = CPR_PRISON_RELIGION_CREATED),
  JsonSubTypes.Type(value = CprReligionUpdated::class, name = CPR_PRISON_RELIGION_UPDATED),
  JsonSubTypes.Type(value = CprContactCreated::class, name = CPR_PRISON_CONTACT_CREATED),
  JsonSubTypes.Type(value = CprContactUpdated::class, name = CPR_PRISON_CONTACT_UPDATED),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface DomainEvent {
  val eventType: String
}
