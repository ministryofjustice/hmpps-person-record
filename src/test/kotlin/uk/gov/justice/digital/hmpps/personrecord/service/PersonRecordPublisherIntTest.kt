package uk.gov.justice.digital.hmpps.personrecord.service.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PersonRecordPublisherIntTest : MessagingMultiNodeTestBase() {

  override fun skipDeletePersonMatchVerification(): Boolean = true

  @Test
  @Order(1)
  fun `should publish domain event when person is created`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val firstName = randomName()
    val lastName = randomName()

    probationDomainEventAndResponseSetup(
      NEW_OFFENDER_CREATED,
      ApiResponseSetup(crn = crn, firstName = firstName, lastName = lastName),
    )

    expectOneMessageOn(testOnlyPersonRecordDomainEventsQueue)

    val body = receiveFirstMessageBody()
    assertThat(body).contains(crn)
    assertThat(body).contains("core-person-record.person.created")
  }

  @Test
  @Order(2)
  fun `should publish domain event when person is updated with matching field change`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val originalFirstName = randomName()
    val updatedFirstName = randomName()

    probationDomainEventAndResponseSetup(
      NEW_OFFENDER_CREATED,
      ApiResponseSetup(crn = crn, firstName = originalFirstName, lastName = randomName()),
    )

    expectOneMessageOn(testOnlyPersonRecordDomainEventsQueue)
    receiveFirstMessageBody()

    purgeQueueAndDlq(testOnlyPersonRecordDomainEventsQueue)
    expectNoMessagesOn(testOnlyPersonRecordDomainEventsQueue)

    probationDomainEventAndResponseSetup(
      OFFENDER_PERSONAL_DETAILS_UPDATED,
      ApiResponseSetup(crn = crn, firstName = updatedFirstName, lastName = randomName()),
    )

    expectOneMessageOn(testOnlyPersonRecordDomainEventsQueue)

    val body = receiveFirstMessageBody()
    assertThat(body).contains(crn)
    assertThat(body).contains("core-person-record.person.updated")
  }

  @Test
  @Order(3)
  fun `should not publish domain event when person is updated with no matching field change`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val firstName = randomName()
    val lastName = randomName()

    val apiResponse = ApiResponseSetup(crn = crn, firstName = firstName, lastName = lastName)

    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

    expectOneMessageOn(testOnlyPersonRecordDomainEventsQueue)
    receiveFirstMessageBody()
    purgeQueueAndDlq(testOnlyPersonRecordDomainEventsQueue)

    probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, apiResponse)

    expectNoMessagesOn(testOnlyPersonRecordDomainEventsQueue)
  }

  @Test
  @Order(4)
  fun `should publish domain event when person is deleted`() {
    stubDeletePersonMatch()

    val crn = randomCrn()
    createPersonWithNewKey(createRandomProbationPersonDetails(crn))

    publishProbationDomainEvent(OFFENDER_DELETION, crn)

    expectOneMessageOn(testOnlyPersonRecordDomainEventsQueue)

    val body = receiveFirstMessageBody()
    assertThat(body).contains(crn)
    assertThat(body).contains("core-person-record.person.deleted")
  }

  private fun receiveFirstMessageBody(): String {
    val message =
      testOnlyPersonRecordDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder()
          .queueUrl(testOnlyPersonRecordDomainEventsQueue!!.queueUrl)
          .maxNumberOfMessages(1)
          .build(),
      )
    return message?.get()?.messages()?.firstOrNull()?.body() ?: ""
  }
}
