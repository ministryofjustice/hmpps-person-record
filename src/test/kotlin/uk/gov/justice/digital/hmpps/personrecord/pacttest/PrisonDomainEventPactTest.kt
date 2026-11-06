package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference

@Provider("hmpps-person-record")
@PactBroker(url = $$"${pactbroker.url}")
@Suppress("unused")
class PrisonDomainEventPactTest : AbstractEventProviderPactTests() {
  private val baseUrl = "http://localhost:8080"

  @State("A prison person created event can be produced")
  fun aPrisonPersonCreatedEventCanBeProduced() {
  }

  @PactVerifyProvider("A CPR person has been created")
  fun verifyCprPersonCreatedEvent(): CprPersonCreated {
    val prisonNumber = "A1234BC"
    val detailUrl = "$baseUrl/person/prison/$prisonNumber"

    return CprPersonCreated(
      eventType = CPR_PRISON_PERSON_CREATED,
      description = "A prison person record has been created",
      detailUrl = detailUrl,
      occurredAt = "2026-10-05T10:00:00+01:00",
      personReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier("prisonNumber", prisonNumber),
        ),
      ),
    )
  }
}
