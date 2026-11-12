package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.PrisonPersonCreatedEventPublisher

@Provider("hmpps-person-record-events")
@PactBroker(url = $$"${pactbroker.url}")
@IgnoreNoPactsToVerify
@Suppress("unused")
class PrisonDomainEventPactTest : AbstractEventProviderPactTests() {
  @Autowired
  private lateinit var prisonPersonCreatedEventPublisher: PrisonPersonCreatedEventPublisher

  @State("A prison person created event can be produced")
  fun aPrisonPersonCreatedEventCanBeProduced() {
  }

  @PactVerifyProvider("A CPR person has been created")
  fun verifyCprPersonCreatedEvent(): String {
    val person = createPersonWithNewKey(createRandomPrisonPersonDetails())
    prisonPersonCreatedEventPublisher.onCreate(PersonCreated(person))

    return jsonMapper.writeValueAsString(receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue, CPR_PRISON_PERSON_CREATED))
  }
}
