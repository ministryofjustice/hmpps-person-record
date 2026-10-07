package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.ProbationAddressEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.ProbationPersonMergedEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.ProbationPersonUpdatedEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import java.util.UUID

/**
 * Pact provider verification for CPR probation domain events.
 *
 * Uses the Pact Broker to load consumer-generated pacts and verifies that CPR produces
 * the expected events when real probation operations occur. Event payloads are
 * captured at the DomainEventPublisher boundary (before SNS) and returned
 * serialized using production JSON configuration.
 *
 * This test exposes incompatibilities as-is: e.g., the merge event currently
 * emits "fromCRN" and "toCRN" identifiers, while some consumers expect a single "CRN".
 */
@Provider("hmpps-person-record-events")
@PactBroker(url = $$"${pactbroker.url}")
@Suppress("unused")
class ProbationEventProviderPactTest : E2ETestBase() {

  @Autowired
  private lateinit var probationAddressEventPublisher: ProbationAddressEventPublisher

  @Autowired
  private lateinit var probationPersonUpdatedEventPublisher: ProbationPersonUpdatedEventPublisher

  @Autowired
  private lateinit var probationPersonMergedEventPublisher: ProbationPersonMergedEventPublisher

  @MockitoSpyBean
  private lateinit var spyDomainEventPublisher: DomainEventPublisher

  private val pactProviderScanPackages = listOf("uk.gov.justice.digital.hmpps.personrecord.pacttest")
  private var capturedDomainEvent: DomainEvent? = null

  @BeforeEach
  fun setUpPactVerification(context: PactVerificationContext) {
    context.target = MessageTestTarget(pactProviderScanPackages, javaClass.classLoader)
    capturedDomainEvent = null
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun testTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }

  @State("CPR publishes a core-person-record.probation.address.created event")
  fun probationAddressCreatedState() {
  }

  @State("CPR publishes a core-person-record.probation.address.updated event")
  fun probationAddressUpdatedState() {
  }

  @State("CPR publishes a core-person-record.probation.address.deleted event")
  fun probationAddressDeletedState() {
  }

  @State("CPR publishes a core-person-record.probation.record.updated event")
  fun probationPersonUpdatedState() {
  }

  @State("CPR publishes a core-person-record.probation.record.merged event")
  fun probationPersonMergedState() {
  }

  @PactVerifyProvider(CPR_PROBATION_ADDRESS_CREATED)
  fun verifyProbationAddressCreatedEvent(): String {
    capturePublishedEvent()
    val crn = randomCrn()
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))
    val address = createRandomProbationAddress()
    val addressEntity = AddressEntity.from(Address.from(address))
    addressEntity.person = person
    addressRepository.save(addressEntity)

    probationAddressEventPublisher.onCreate(AddressCreated(addressEntity, eventSource = CPR))

    return captureLastPublishedEvent()
  }

  @PactVerifyProvider(CPR_PROBATION_ADDRESS_UPDATED)
  fun verifyProbationAddressUpdatedEvent(): String {
    capturePublishedEvent()
    val crn = randomCrn()
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))
    val address = createRandomProbationAddress()
    val addressEntity = AddressEntity.from(Address.from(address))
    addressEntity.person = person
    addressRepository.save(addressEntity)

    probationAddressEventPublisher.onUpdate(AddressUpdated(addressEntity, eventSource = CPR))

    return captureLastPublishedEvent()
  }

  @PactVerifyProvider(CPR_PROBATION_ADDRESS_DELETED)
  fun verifyProbationAddressDeletedEvent(): String {
    capturePublishedEvent()
    val crn = randomCrn()
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))
    val address = createRandomProbationAddress()
    val addressEntity = AddressEntity.from(Address.from(address))
    addressEntity.person = person
    addressRepository.save(addressEntity)

    probationAddressEventPublisher.onDelete(AddressDeleted(addressEntity, person, CPR))

    return captureLastPublishedEvent()
  }

  @PactVerifyProvider(CPR_PROBATION_PERSON_UPDATED)
  fun verifyProbationPersonUpdatedEvent(): String {
    capturePublishedEvent()
    val crn = randomCrn()
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))

    personRepository.flush()
    val reloaded = personRepository.findByMatchId(person.matchId)!!
    val changeChecker = PersonChangeChecker(reloaded)

    probationPersonUpdatedEventPublisher.onUpdate(PersonUpdated(reloaded, changeChecker))

    return captureLastPublishedEvent()
  }

  @PactVerifyProvider(CPR_PROBATION_PERSON_MERGED)
  fun verifyProbationPersonMergedEvent(): String {
    capturePublishedEvent()
    val fromCrn = randomCrn()
    val toCrn = randomCrn()
    val fromPerson = createPersonWithNewKey(createRandomProbationPersonDetails(fromCrn))
    val toPerson = createPersonWithNewKey(createRandomProbationPersonDetails(toCrn))

    probationPersonMergedEventPublisher.onMerge(
      PersonMerged(
        from = fromPerson,
        fromClusterDetail = EventLogClusterDetail(UUID.randomUUID(), ACTIVE),
        to = toPerson,
      ),
    )

    return captureLastPublishedEvent()
  }

  private fun capturePublishedEvent() {
    capturedDomainEvent = null
    doAnswer { invocation ->
      capturedDomainEvent = invocation.getArgument(0)
      null
    }.whenever(spyDomainEventPublisher).publish(any(), any())
  }

  private fun captureLastPublishedEvent(): String {
    val domainEvent = requireNotNull(capturedDomainEvent) { "Expected a domain event to be published" }
    return jsonMapper.writeValueAsString(domainEvent)
  }
}
