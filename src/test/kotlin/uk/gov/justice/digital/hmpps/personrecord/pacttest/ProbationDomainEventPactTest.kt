package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import java.util.UUID

@PactFolder("src/test/resources/pacts/sas-probation-domain-events")
@Suppress("unused", "SameParameterValue")
class ProbationDomainEventPactTest : AbstractEventProviderPactTests() {
  private val baseUrl = "http://localhost:8080"
  private val crn = "X123456"
  private val fromCrn = "X654321"
  private val toCrn = "X123456"
  private val createdAddressId = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val updatedAddressId = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val deletedAddressId = UUID.fromString("33333333-3333-3333-3333-333333333333")
  private val deliusAddressId = 12345L
  private val occurredAt = "2026-10-05T10:00:00+01:00"

  @State("A probation address created event can be produced for SAS")
  fun probationAddressCreatedState() {
  }

  @State("A probation address updated event can be produced for SAS")
  fun probationAddressUpdatedState() {
  }

  @State("A probation address deleted event can be produced for SAS")
  fun probationAddressDeletedState() {
  }

  @State("A probation person updated event can be produced for SAS")
  fun probationPersonUpdatedState() {
  }

  @State("A probation person merged event can be produced for SAS")
  fun probationPersonMergedState() {
  }

  @PactVerifyProvider("A CPR probation address created event")
  fun verifyProbationAddressCreatedEvent(): CprAddressCreated = CprAddressCreated(
    eventType = CPR_PROBATION_ADDRESS_CREATED,
    occurredAt = occurredAt,
    description = "A probation address has been created for a person",
    detailUrl = "$baseUrl/person/probation/$crn/address/$createdAddressId",
    personReference = crnReference(crn),
    additionalInformation = CprAddressCreatedInfo(
      cprAddressId = createdAddressId,
      deliusAddressId = deliusAddressId,
    ),
  )

  @PactVerifyProvider("A CPR probation address updated event")
  fun verifyProbationAddressUpdatedEvent(): CprAddressUpdated = CprAddressUpdated(
    eventType = CPR_PROBATION_ADDRESS_UPDATED,
    occurredAt = occurredAt,
    description = "A probation address has been updated for a person",
    detailUrl = "$baseUrl/person/probation/$crn/address/$updatedAddressId",
    personReference = crnReference(crn),
    additionalInformation = CprAddressUpdatedInfo(
      cprAddressId = updatedAddressId,
      deliusAddressId = deliusAddressId,
    ),
  )

  @PactVerifyProvider("A CPR probation address deleted event")
  fun verifyProbationAddressDeletedEvent(): CprAddressDeleted = CprAddressDeleted(
    eventType = CPR_PROBATION_ADDRESS_DELETED,
    occurredAt = occurredAt,
    description = "A probation address has been deleted for a person",
    personReference = crnReference(crn),
    additionalInformation = CprAddressDeletedInfo(
      cprAddressId = deletedAddressId,
      deliusAddressId = deliusAddressId,
    ),
  )

  @PactVerifyProvider("A CPR probation person updated event")
  fun verifyProbationPersonUpdatedEvent(): CprPersonUpdated = CprPersonUpdated(
    eventType = CPR_PROBATION_PERSON_UPDATED,
    occurredAt = occurredAt,
    description = "A probation person record has been updated",
    detailUrl = "$baseUrl/person/probation/$crn",
    personReference = crnReference(crn),
  )

  @PactVerifyProvider("A CPR probation person merged event")
  fun verifyProbationPersonMergedEvent(): CprPersonMerged = CprPersonMerged(
    eventType = CPR_PROBATION_PERSON_MERGED,
    occurredAt = occurredAt,
    description = "A probation person record has been merged",
    detailUrl = "$baseUrl/person/probation/$toCrn",
    personReference = PersonReference(
      identifiers = listOf(
        PersonIdentifier("fromCRN", fromCrn),
        PersonIdentifier("toCRN", toCrn),
      ),
    ),
  )

  private fun crnReference(crn: String) = PersonReference(
    identifiers = listOf(
      PersonIdentifier("CRN", crn),
    ),
  )
}
