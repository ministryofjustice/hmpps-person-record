package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
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
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "eventType",
  visible = true,
)
@JsonSubTypes(
  JsonSubTypes.Type(value = ProbationOffenderCreated::class, name = NEW_OFFENDER_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderUpdated::class, name = OFFENDER_PERSONAL_DETAILS_UPDATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressCreated::class, name = OFFENDER_ADDRESS_CREATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressUpdated::class, name = OFFENDER_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = ProbationOffenderAddressDeleted::class, name = OFFENDER_ADDRESS_DELETED),
  JsonSubTypes.Type(value = ProbationOffenderMerged::class, name = OFFENDER_MERGED),
  JsonSubTypes.Type(value = ProbationOffenderUnMerged::class, name = OFFENDER_UNMERGED),
  JsonSubTypes.Type(value = ProbationOffenderDeleted::class, names = [OFFENDER_DELETION, OFFENDER_GDPR_DELETION]),
  JsonSubTypes.Type(value = PrisonPrisonerCreated::class, name = PRISONER_CREATED),
  JsonSubTypes.Type(value = PrisonPrisonerUpdated::class, name = PRISONER_UPDATED),
  JsonSubTypes.Type(value = PrisonPrisonerMerged::class, name = PRISONER_MERGED),
  JsonSubTypes.Type(value = SasAddressUpdated::class, name = SAS_ADDRESS_UPDATED),
  JsonSubTypes.Type(value = CprPersonCreated::class, names = [CPR_PRISON_PERSON_CREATED, CPR_PROBATION_PERSON_CREATED, CPR_COURT_PERSON_CREATED]),
  JsonSubTypes.Type(value = CprAddressCreated::class, name = CPR_PROBATION_ADDRESS_CREATED),
  JsonSubTypes.Type(value = CprAddressUpdated::class, name = CPR_PROBATION_ADDRESS_UPDATED),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface DomainEvent {
  val eventType: String
  val version: Int
  val occurredAt: String
}
