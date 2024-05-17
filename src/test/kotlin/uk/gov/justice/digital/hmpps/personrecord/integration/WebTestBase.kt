package uk.gov.justice.digital.hmpps.personrecord.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.security.JwtAuthHelper
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Duration

abstract class WebTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(): WebTestClient.RequestBodySpec {
    val bearerToken = jwtHelper.createJwt(
      subject = "hmpps-person-record",
      expiryTime = Duration.ofMinutes(1L),
      roles = listOf(),
    )
    return header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec
  }

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var personRepository: PersonRepository

  val cprCourtCaseEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }

  val cprDeliusOffenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAll()
    cprCourtCaseEventsQueue?.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprCourtCaseEventsQueue?.dlqUrl).build(),
    ).get()
    cprCourtCaseEventsQueue?.sqsClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprCourtCaseEventsQueue?.queueUrl).build(),
    ).get()
    cprDeliusOffenderEventsQueue?.sqsClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue?.queueUrl).build(),
    )
    cprDeliusOffenderEventsQueue?.sqsDlqClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue?.dlqUrl).build(),
    )
  }
}
