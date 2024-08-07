package uk.gov.justice.digital.hmpps.personrecord.config

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.health.HealthInfo
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTestBase {

  @Autowired
  lateinit var telemetryRepository: TelemetryTestRepository

  @Autowired
  private lateinit var buildProperties: BuildProperties

  @MockBean
  @Autowired
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  @Autowired
  private lateinit var healthInfo: HealthInfo

  @BeforeEach
  fun setup() {
    `when`(personMatchHealthPing.health()).thenReturn(Health.up().build())
    `when`(personRecordHealthPing.health()).thenReturn(Health.up().build())

    healthInfo = HealthInfo(buildProperties)
  }

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String>,
    times: Int = 1,
  ) {
    await.atMost(3, SECONDS) untilAsserted {
      val allEvents = telemetryRepository.findAllByEvent(event.eventName)
      val matchingEvents = allEvents?.filter {
        expected.entries.map { (k, v) ->
          val jsonObject = JSONObject(it.properties)
          when {
            (jsonObject.has(k)) -> jsonObject.get(k).equals(v)
            else -> false
          }
        }.all { it }
      }
      assertThat(matchingEvents?.size).`as`("Missing data $event $expected and actual data $allEvents").isEqualTo(times)
    }
  }
  companion object {

    @JvmStatic
    @RegisterExtension
    var wiremock: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .failOnUnmatchedRequests(true)
      .build()
  }
}
