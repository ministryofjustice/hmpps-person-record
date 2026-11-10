package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.PrisonPersonCreatedEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Provider("hmpps-person-record-events")
@PactBroker(url = $$"${pactbroker.url}")
@Suppress("unused")
class PrisonDomainEventPactTest : AbstractEventProviderPactTests() {
  @Autowired
  private lateinit var prisonPersonCreatedEventPublisher: PrisonPersonCreatedEventPublisher

  @MockitoSpyBean
  private lateinit var spyDomainEventPublisher: DomainEventPublisher

  @State("A prison person created event can be produced")
  fun aPrisonPersonCreatedEventCanBeProduced() {
  }

  @PactVerifyProvider("A CPR person has been created")
  fun verifyCprPersonCreatedEvent(): String {
    var capturedDomainEvent: DomainEvent? = null
    doAnswer { invocation ->
      capturedDomainEvent = invocation.getArgument(0)
      null
    }.`when`(spyDomainEventPublisher).publish(any(), any())

    val person = createPersonWithNewKey(createRandomPrisonPersonDetails())
    prisonPersonCreatedEventPublisher.onCreate(PersonCreated(person))

    val domainEvent = requireNotNull(capturedDomainEvent) { "Expected a prison person created event to be published" }
    return jsonMapper.writeValueAsString(domainEvent)
  }
}
