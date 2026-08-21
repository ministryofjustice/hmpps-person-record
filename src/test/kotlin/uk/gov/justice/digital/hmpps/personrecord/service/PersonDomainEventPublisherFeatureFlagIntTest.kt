package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
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

@ActiveProfiles("prod")
class PersonDomainEventPublisherFeatureFlagIntTest : MessagingTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish a CPR probation person created domain event in prod`() {
    val crn = randomCrn()
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(crn)))
    awaitNotNull { personRepository.findByCrn(crn) }
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
  }

  @Test
  fun `should not publish a CPR prison person created domain event in prod`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(
      PrisonPersonCreated(
        personReference = PersonReference(
          listOf(
            PersonIdentifier(
              "NOMS",
              prisonNumber,
            ),
          ),
        ),
      ),
    )
    awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
  }

  @Test
  fun `should not publish a CPR common platform person created domain event in prod`() {
    val defendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            defendantId = defendantId,
            cro = randomCro(),
            pnc = randomLongPnc(),
          ),
        ),
      ),
    )
    awaitNotNull { personRepository.findByDefendantId(defendantId) }
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
  }

  @Test
  fun `should not publish a CPR libra person created domain event in prod`() {
    val cid = randomCId()
    publishLibraMessage(libraHearing(cId = cid, firstName = randomName(), lastName = randomName(), defendantType = DefendantType.PERSON))
    awaitNotNull { personRepository.findByCId(cid) }
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
  }
}
