package uk.gov.justice.digital.hmpps.personrecord.integration

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

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

  companion object {

    @JvmStatic
    @RegisterExtension
    var wireMockExtension: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .failOnUnmatchedRequests(true)
      .build()
  }
}
