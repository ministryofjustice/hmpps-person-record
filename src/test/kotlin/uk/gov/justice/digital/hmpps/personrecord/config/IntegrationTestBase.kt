package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.json.JSONObject
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.health.HealthInfo
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTestBase {

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var personKeyRepository: PersonKeyRepository

  @Autowired
  lateinit var personRepository: PersonRepository

  @Autowired
  lateinit var matchScoreClient: MatchScoreClient

  @Autowired
  lateinit var telemetryRepository: TelemetryTestRepository

  @Autowired
  lateinit var telemetryClient: TelemetryClient

  @Autowired
  private lateinit var buildProperties: BuildProperties

  @Autowired
  private lateinit var healthInfo: HealthInfo

  @Autowired
  lateinit var eventLoggingRepository: EventLoggingRepository

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String?>,
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

  internal fun awaitAssert(function: () -> Unit) =
    await atMost(Duration.ofSeconds(2)) untilAsserted function

  internal fun awaitNotNullPerson(function: () -> PersonEntity?): PersonEntity =
    await atMost (Duration.ofSeconds(2)) untilNotNull function

  internal fun createPersonKey(status: UUIDStatusType = UUIDStatusType.ACTIVE): PersonKeyEntity {
    val personKeyEntity = PersonKeyEntity.new()
    personKeyEntity.status = status
    return personKeyRepository.saveAndFlush(personKeyEntity)
  }

  internal fun createPerson(person: Person, personKeyEntity: PersonKeyEntity? = null): PersonEntity {
    val personEntity = PersonEntity.from(person = person)
    personEntity.personKey = personKeyEntity
    return personRepository.saveAndFlush(personEntity)
  }

  internal fun mergeRecord(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.mergedTo = targetPersonEntity.id
    personRepository.saveAndFlush(sourcePersonEntity)
  }

  internal fun mergeUuid(sourcePersonKey: PersonKeyEntity, targetPersonKeyEntity: PersonKeyEntity): PersonKeyEntity {
    sourcePersonKey.mergedTo = targetPersonKeyEntity.id
    if (sourcePersonKey.personEntities.size == 1) sourcePersonKey.status = UUIDStatusType.MERGED
    return personKeyRepository.saveAndFlush(sourcePersonKey)
  }

  internal fun excludeRecord(sourceRecord: PersonEntity, excludingRecord: PersonEntity) {
    sourceRecord.overrideMarkers.add(
      OverrideMarkerEntity(
        markerType = OverrideMarkerType.EXCLUDE,
        markerValue = excludingRecord.id,
        person = sourceRecord,
      ),
    )
    excludingRecord.overrideMarkers.add(
      OverrideMarkerEntity(
        markerType = OverrideMarkerType.EXCLUDE,
        markerValue = sourceRecord.id,
        person = sourceRecord,
      ),
    )
    personRepository.saveAndFlush(excludingRecord)
    personRepository.saveAndFlush(sourceRecord)
  }

  internal fun stubMatchScore(matchResponse: MatchResponse, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) {
    wiremock.stubFor(
      WireMock.post("/person/match")
        .inScenario(scenario)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }

  companion object {

    internal const val BASE_SCENARIO = "baseScenario"

    @JvmStatic
    @RegisterExtension
    var wiremock: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .failOnUnmatchedRequests(true)
      .build()
  }
}
