package uk.gov.justice.digital.hmpps.personrecord.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.personrecord.integration.LocalStackHelper.setLocalStackProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class SqsIntegrationTestBase {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  // TODO add queues and topics

  @BeforeEach
  fun cleanQueue() {
    // TODO purge queues
  }

  companion object {
    private val localStackContainer = LocalStackHelper.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
