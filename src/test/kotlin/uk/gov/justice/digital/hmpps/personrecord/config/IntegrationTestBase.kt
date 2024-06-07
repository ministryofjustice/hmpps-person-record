package uk.gov.justice.digital.hmpps.personrecord.config

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTestBase {

  @Autowired
  lateinit var telemetryRepository: TelemetryTestRepository

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String>,
    times: Int = 1,
  ) {
    await.atMost(1, SECONDS) untilAsserted {
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
      Assertions.assertThat(matchingEvents?.size).`as`("Missing data $event $expected and actual data $allEvents").isEqualTo(times)
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

  @BeforeEach
  fun beforeEach() {
    wiremock.stubFor(
      WireMock.post("/auth/oauth/token")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              "{\n" +
                "  \"access_token\": \"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwcm9iYXRpb24taW50ZWdyYXRpb24tZGV2IiwiZ3JhbnRfdHlwZSI6ImNsaWVudF9jcmVkZW50aWFscyIsInVzZXJfbmFtZSI6InByb2JhdGlvbi1pbnRlZ3JhdGlvbi1kZXYiLCJzY29wZSI6WyJyZWFkIiwid3JpdGUiXSwiYXV0aF9zb3VyY2UiOiJub25lIiwiaXNzIjoiaHR0cHM6Ly9zaWduLWluLWRldi5obXBwcy5zZXJ2aWNlLmp1c3RpY2UuZ292LnVrL2F1dGgvaXNzdWVyIiwiZXhwIjo5OTk5OTk5OTk5LCJhdXRob3JpdGllcyI6WyJST0xFX0FQUFJPVkVEX1BSRU1JU0VTX0FTU0VTU01FTlRTIl0sImp0aSI6IjI1RHVSbjEtaHlIWmV3TGNkSkp4d1ZMMDNLVSIsImNsaWVudF9pZCI6InByb2JhdGlvbi1pbnRlZ3JhdGlvbi1kZXYiLCJpYXQiOjE2NjM3NTczMTF9.teMLHccVosDs8-v2nhsfiYg66TcL44oKtmNlVAybQncrWRG66LpSkZyf7ashCEuYrSd_Thqh1JRQsrLtWdGAA384tAw-kRQOG-67HYsXWXUn3O8VizxSJM-Ng75IeHBX5c3eYlhYYhxFYoL4H6uMuQog4BaBaHD63ZgovPhG3Pw\",\n" +
                "  \"token_type\": \"bearer\",\n" +
                "  \"expires_in\": 9999999999,\n" +
                "  \"scope\": \"read write\",\n" +
                "  \"sub\": \"hmpps-person-record-dev\",\n" +
                "  \"auth_source\": \"none\",\n" +
                "  \"jti\": \"fN29JHJy1N7gcYvqe-8B_k5T0mA\",\n" +
                "  \"iss\": \"https://sign-in-dev.hmpps.service.justice.gov.uk/auth/issuer\"\n" +
                "}",
            ),
        ),
    )
  }
}
