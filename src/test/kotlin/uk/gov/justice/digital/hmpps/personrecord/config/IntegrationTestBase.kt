package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.json.JSONObject
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType.EXCLUDE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import java.time.Duration
import java.util.UUID

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
  lateinit var telemetryRepository: TelemetryTestRepository

  @Autowired
  lateinit var eventLoggingRepository: EventLoggingRepository

  fun probationUrl(crn: String) = "/probation-cases/$crn"

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String?>,
    times: Int = 1,
  ) {
    awaitAssert {
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

  internal fun awaitAssert(function: () -> Unit) = await atMost (Duration.ofSeconds(3)) untilAsserted function

  internal fun awaitNotNullPerson(function: () -> PersonEntity?): PersonEntity = await atMost (Duration.ofSeconds(3)) untilNotNull function

  internal fun awaitNotNullEventLog(sourceSystemId: String, eventType: String) = await atMost (Duration.ofSeconds(3)) untilNotNull {
    eventLoggingRepository.findFirstBySourceSystemIdAndEventTypeOrderByEventTimestampDesc(sourceSystemId, eventType)
  }
  internal fun createPersonKey(status: UUIDStatusType = ACTIVE): PersonKeyEntity {
    val personKeyEntity = PersonKeyEntity.new()
    personKeyEntity.status = status
    return personKeyRepository.save(personKeyEntity)
  }

  internal fun createPersonWithNewKey(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person = person)
    personEntity.personKey = createPersonKey()
    return personRepository.saveAndFlush(personEntity)
  }

  internal fun createPerson(person: Person, personKeyEntity: PersonKeyEntity? = null): PersonEntity {
    val personEntity = PersonEntity.new(person = person)
    personEntity.personKey = personKeyEntity
    return personRepository.saveAndFlush(personEntity)
  }

  internal fun mergeRecord(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.mergedTo = targetPersonEntity.id
    personRepository.saveAndFlush(sourcePersonEntity)
  }

  internal fun mergeUuid(sourcePersonKey: PersonKeyEntity, targetPersonKeyEntity: PersonKeyEntity): PersonKeyEntity {
    sourcePersonKey.mergedTo = targetPersonKeyEntity.id
    if (sourcePersonKey.personEntities.size == 1) sourcePersonKey.status = MERGED
    return personKeyRepository.saveAndFlush(sourcePersonKey)
  }

  internal fun excludeRecord(sourceRecord: PersonEntity, excludingRecord: PersonEntity) {
    sourceRecord.overrideMarkers.add(
      OverrideMarkerEntity(
        markerType = EXCLUDE,
        markerValue = excludingRecord.id,
        person = sourceRecord,
      ),
    )
    excludingRecord.overrideMarkers.add(
      OverrideMarkerEntity(
        markerType = EXCLUDE,
        markerValue = sourceRecord.id,
        person = sourceRecord,
      ),
    )
    personRepository.saveAndFlush(excludingRecord)
    personRepository.saveAndFlush(sourceRecord)
  }

  internal fun stubOneHighConfidenceMatch() = stubXHighConfidenceMatches(1)

  internal fun stubOneLowConfidenceMatch() = stubMatchScore(
    MatchResponse(matchProbabilities = mutableMapOf("0" to 0.988899)),
  )

  internal fun stubXHighConfidenceMatches(x: Int) = stubMatchScore(
    MatchResponse(
      matchProbabilities = (0..<x).associate {
        Pair("$it", 0.999999)
      }.toMutableMap(),
    ),
  )

  internal fun stubMatchScore(matchResponse: MatchResponse, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED, status: Int = 200) {
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      urlPattern = urlEqualTo("/person/match"),
      status = status,
      body = objectMapper.writeValueAsString(matchResponse),
    )
  }

  internal fun stubPersonMatchScores(matchId: UUID? = null, personMatchResponse: List<PersonMatchScore> = listOf(), scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED, status: Int = 200) {
    val matchIdUrlPattern: UrlPattern = matchId?.let { urlEqualTo("/person/score/$it") } ?: urlPathMatching("/person/score/.*") // Regex to match any matchId, as not known on create
    val responseBody: List<PersonMatchScore> = personMatchResponse.isEmpty().let { listOf(PersonMatchScore(candidateMatchId = matchId.toString(), candidateMatchWeight = 1.0F, candidateMatchProbability = 1.0F)) }
    stubGetRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      urlPattern = matchIdUrlPattern,
      status = status,
      body = objectMapper.writeValueAsString(responseBody),
    )
  }

  internal fun stubPersonMatch(scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED, status: Int = 200, body: String = "{}") {
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      urlPattern = urlEqualTo("/person"),
      status = status,
      body = body,
    )
  }

  internal fun stubDeletePersonMatch(scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED, status: Int = 200, body: String = "{}") {
    stubDeleteRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      urlPattern = urlEqualTo("/person"),
      status = status,
      body = body,
    )
  }

  fun stub404Response(url: String) {
    wiremock.stubFor(
      WireMock.get(url)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  internal fun stubGetRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, urlPattern: UrlPattern, body: String, status: Int = 200) {
    wiremock.stubFor(
      WireMock.get(urlPattern)
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(body),
        ),
    )
  }

  internal fun stubPostRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, urlPattern: UrlPattern, body: String, status: Int = 200) {
    wiremock.stubFor(
      WireMock.post(urlPattern)
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(body),
        ),
    )
  }

  internal fun stubDeleteRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, urlPattern: UrlPattern, body: String, status: Int = 200) {
    wiremock.stubFor(
      WireMock.delete(urlPattern)
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(body),
        ),
    )
  }

  fun blitz(actionCount: Int, threadCount: Int, action: () -> Unit) {
    val blitzer = Blitzer(actionCount, threadCount)
    try {
      blitzer.blitz {
        action()
      }
    } finally {
      blitzer.shutdown()
    }
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
