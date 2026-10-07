package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import java.util.UUID

/**
 * Lightweight local smoke test for probation domain events.
 *
 * Uses the Pact Broker to load consumer-generated pacts and verifies that
 * CPR produces events correctly. This test should stay minimal for quick
 * local developer feedback.
 */
@Provider("hmpps-person-record")
@PactBroker(url = $$"${pactbroker.url}")
@Suppress("unused")
class ProbationDomainEventPactTest : AbstractEventProviderPactTests() {
  private val baseUrl = "http://localhost:8080"

  @State("CPR publishes a probation address event")
  fun probationAddressEventState() {
  }

  @PactVerifyProvider(CPR_PROBATION_ADDRESS_CREATED)
  fun verifyProbationAddressCreatedEvent(): CprAddressCreated {
    val crn = "X123456"
    val addressId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    return CprAddressCreated(
      eventType = CPR_PROBATION_ADDRESS_CREATED,
      occurredAt = "2026-10-05T10:00:00+01:00",
      description = "A probation address has been created for a person",
      detailUrl = "$baseUrl/person/probation/$crn/address/$addressId",
      personReference = PersonReference(identifiers = listOf(PersonIdentifier("CRN", crn))),
      additionalInformation = CprAddressCreatedInfo(cprAddressId = addressId, deliusAddressId = null),
    )
  }
}
