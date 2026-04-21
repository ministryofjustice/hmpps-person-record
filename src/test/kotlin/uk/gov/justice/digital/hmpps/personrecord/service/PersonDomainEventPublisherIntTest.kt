package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonDomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PersonDomainEventPublisherIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish a CPR person created domain event when a person is created in nomis`() {
    val prisonNumber = randomPrisonNumber()

    prisonDomainEventAndResponseSetup(
      PRISONER_CREATED,
      apiResponseSetup = ApiResponseSetup(prisonNumber = prisonNumber),
    )

    awaitNotNull {
      personRepository.findByPrisonNumber(prisonNumber)
    }

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_PERSON_CREATED))
    val personDomainEvent: PersonDomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(personDomainEvent.eventType).isEqualTo(CPR_PRISON_PERSON_CREATED)
    assertThat(personDomainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber")
    assertThat(personDomainEvent.description).isEqualTo("A prison person record has been created")
    assertThat(personDomainEvent.occurredAt).isNotNull()
    assertThat(personDomainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.type).isEqualTo("NOMS")
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  @Test
  fun `should publish a CPR person created domain event when a person is created in delius`() {
    val crn = randomCrn()

    probationDomainEventAndResponseSetup(
      NEW_OFFENDER_CREATED,
      ApiResponseSetup.from(createRandomProbationCase(crn)),
    )

    awaitNotNull {
      personRepository.findByCrn(crn)
    }

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_CREATED))
    val personDomainEvent: PersonDomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(personDomainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_CREATED)
    assertThat(personDomainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn")
    assertThat(personDomainEvent.description).isEqualTo("A probation person record has been created")
    assertThat(personDomainEvent.occurredAt).isNotNull()
    assertThat(personDomainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.type).isEqualTo("CRN")
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.value).isEqualTo(crn)
  }

  @Test
  fun `should publish a CPR person created domain event when a person is created in common platform`() {
    val defendantId = randomDefendantId()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = randomCro(), pnc = randomLongPnc()))),
    )

    awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_CREATED))
    val personDomainEvent: PersonDomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(personDomainEvent.eventType).isEqualTo(CPR_COURT_PERSON_CREATED)
    assertThat(personDomainEvent.detailUrl).isEqualTo("http://localhost:8080/person/commonplatform/$defendantId")
    assertThat(personDomainEvent.description).isEqualTo("A court person record has been created")
    assertThat(personDomainEvent.occurredAt).isNotNull()
    assertThat(personDomainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.type).isEqualTo("DEFENDANT_ID")
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.value).isEqualTo(defendantId)
  }

  @Test
  fun `should publish a CPR person created domain event when a person is created in libra`() {
    val cid = randomCId()

    publishLibraMessage(libraHearing(cId = cid, firstName = randomName(), lastName = randomName(), defendantType = PERSON))

    awaitNotNull {
      personRepository.findByCId(cid)
    }

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_CREATED))
    val personDomainEvent: PersonDomainEvent = jsonMapper.readValue(sqsMessage.message)
    assertThat(personDomainEvent.eventType).isEqualTo(CPR_COURT_PERSON_CREATED)
    assertThat(personDomainEvent.detailUrl).isEqualTo("http://localhost:8080/person/libra/$cid")
    assertThat(personDomainEvent.description).isEqualTo("A court person record has been created")
    assertThat(personDomainEvent.occurredAt).isNotNull()
    assertThat(personDomainEvent.personReference?.identifiers?.size).isEqualTo(1)
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.type).isEqualTo("C_ID")
    assertThat(personDomainEvent.personReference?.identifiers?.get(0)?.value).isEqualTo(cid)
  }
}
