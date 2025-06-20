package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.RequestMethod.DELETE
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType.EXCLUDE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService.Companion.THRESHOLD_WEIGHT
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name as LibraName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name as OffenderName

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
  lateinit var eventLogRepository: EventLogRepository

  fun authSetup() {
    wiremock.stubFor(
      WireMock.post("/auth/oauth/token")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
                {
                  "token_type": "bearer",
                  "access_token": "SOME_TOKEN",
                  "expires_in": ${LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toEpochSecond(ZoneOffset.UTC)}
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  @AfterEach
  fun after() {
    wiremock.stubMappings.forEach {
      when (it.request.method) {
        GET -> if (it.request.url != null) {
          wiremock.verify(getRequestedFor(urlEqualTo(it.request.url)))
        } else {
          wiremock.verify(getRequestedFor(urlMatching(it.request.urlPathPattern)))
        }
        POST -> if (it.request.url != "/auth/oauth/token") {
          wiremock.verify(postRequestedFor(urlEqualTo(it.request.url)))
        }
        DELETE -> wiremock.verify(deleteRequestedFor(urlEqualTo(it.request.url)))
        PUT -> wiremock.verify(putRequestedFor(urlEqualTo(it.request.url)))
        else -> fail()
      }
    }
  }

  fun probationUrl(crn: String) = "/probation-cases/$crn"

  internal fun createRandomProbationPersonDetails(crn: String = randomCrn()): Person = Person.from(ProbationCase(name = OffenderName(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn)))

  internal fun createRandomPrisonPersonDetails(prisonNumber: String = randomPrisonNumber()): Person = Person.from(
    Prisoner(prisonNumber = prisonNumber, firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate()),
  )

  internal fun createRandomLibraPersonDetails(cId: String = randomCId()): Person = Person.from(LibraHearingEvent(name = LibraName(firstName = randomName(), lastName = randomName()), cId = cId))

  internal fun createRandomCommonPlatformPersonDetails(defendantId: String = randomDefendantId()): Person = Person.from(
    Defendant(id = defendantId, personDefendant = PersonDefendant(personDetails = PersonDetails(firstName = randomName(), lastName = randomName()))),
  )

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String?>,
    times: Int = 1,
    timeout: Long = 3,
  ) {
    awaitAssert(function = {
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
    }, timeout)
  }

  internal fun checkEventLogExist(
    sourceSystemId: String,
    event: CPRLogEvents,
    times: Int = 1,
  ) {
    checkEventLog(sourceSystemId, event) { logEvents ->
      assertThat(logEvents).`as`("Missing event log $event and actual data $logEvents").hasSize(times)
    }
  }

  internal fun checkEventLog(
    sourceSystemId: String,
    event: CPRLogEvents,
    timeout: Long = 3,
    matchingEvents: (logEvents: List<EventLogEntity>) -> Unit,
  ) {
    awaitAssert(function = {
      matchingEvents(eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(event, sourceSystemId) ?: emptyList())
    }, timeout)
  }

  internal fun checkEventLogByUUID(
    cluster: UUID,
    event: CPRLogEvents,
    timeout: Long = 3,
    matchingEvents: (logEvents: List<EventLogEntity>) -> Unit,
  ) {
    awaitAssert(function = {
      matchingEvents(eventLogRepository.findAllByEventTypeAndPersonUUIDOrderByEventTimestampDesc(event, cluster) ?: emptyList())
    }, timeout)
  }

  private fun awaitAssert(function: () -> Unit, timeout: Long) = await atMost (Duration.ofSeconds(timeout)) untilAsserted function

  internal fun awaitAssert(function: () -> Unit) = awaitAssert(function = function, timeout = 3)

  internal fun awaitNotNullPerson(function: () -> PersonEntity?): PersonEntity = await atMost (Duration.ofSeconds(3)) untilNotNull function

  internal fun createPersonKey(status: UUIDStatusType = ACTIVE): PersonKeyEntity {
    val personKeyEntity = PersonKeyEntity.new()
    personKeyEntity.status = status
    return personKeyRepository.save(personKeyEntity)
  }

  internal fun PersonKeyEntity.addPerson(personEntity: PersonEntity): PersonKeyEntity {
    this.personEntities.add(personEntity)
    personEntity.personKey = this
    return personKeyRepository.save(this)
  }

  internal fun createPersonWithNewKey(person: Person, status: UUIDStatusType = ACTIVE): PersonEntity = createPerson(person, createPersonKey(status))

  internal fun createPerson(person: Person, personKeyEntity: PersonKeyEntity? = null): PersonEntity {
    val personEntity = PersonEntity.new(person = person)
    personEntity.personKey = personKeyEntity
    personKeyEntity?.personEntities?.add(personEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  internal fun mergeRecord(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity): PersonEntity {
    val source = personRepository.findByMatchId(sourcePersonEntity.matchId)!!
    val target = personRepository.findByMatchId(targetPersonEntity.matchId)!!
    source.mergedTo = target.id
    return personRepository.save(source)
  }

  internal fun mergeUuid(sourcePersonKey: PersonKeyEntity, targetPersonKeyEntity: PersonKeyEntity): PersonKeyEntity {
    val source = personKeyRepository.findByPersonUUID(sourcePersonKey.personUUID)!!
    val target = personKeyRepository.findByPersonUUID(targetPersonKeyEntity.personUUID)!!
    source.mergedTo = target.id
    source.status = MERGED
    return personKeyRepository.saveAndFlush(source)
  }

  internal fun excludeRecord(sourceRecord: PersonEntity, excludingRecord: PersonEntity) {
    val source = personRepository.findByMatchId(sourceRecord.matchId)
    source?.overrideMarkers?.add(
      OverrideMarkerEntity(
        markerType = EXCLUDE,
        markerValue = excludingRecord.id,
        person = sourceRecord,
      ),
    )
    val target = personRepository.findByMatchId(sourceRecord.matchId)
    target?.overrideMarkers?.add(
      OverrideMarkerEntity(
        markerType = EXCLUDE,
        markerValue = sourceRecord.id,
        person = excludingRecord,
      ),
    )
    personRepository.saveAll(listOf(source, target))
  }

  internal fun stubNoMatchesPersonMatch(matchId: UUID? = null) = stubPersonMatchScores(matchId = matchId, personMatchResponse = emptyList())

  internal fun stubOnePersonMatchHighConfidenceMatch(matchId: UUID? = null, matchedRecord: UUID) = stubXPersonMatchHighConfidenceMatches(
    matchId = matchId,
    results = listOf(matchedRecord),
  )

  internal fun stubOnePersonMatchLowConfidenceMatch(matchId: UUID? = null, matchedRecord: UUID) = stubPersonMatchScores(
    matchId = matchId,
    personMatchResponse = listOf(
      PersonMatchScore(
        candidateMatchId = matchedRecord.toString(),
        candidateMatchWeight = THRESHOLD_WEIGHT - 1F,
        candidateMatchProbability = 0.988899F,
      ),
    ),
  )

  internal fun stubXPersonMatchHighConfidenceMatches(matchId: UUID? = null, results: List<UUID>) = stubPersonMatchScores(
    matchId = matchId,
    personMatchResponse = List(results.size) { index ->
      PersonMatchScore(
        candidateMatchId = results[index].toString(),
        candidateMatchWeight = THRESHOLD_WEIGHT + 1F,
        candidateMatchProbability = 0.999999F,
      )
    },
  )

  internal fun stubPersonMatchScores(
    matchId: UUID? = null,
    personMatchResponse: List<PersonMatchScore> = emptyList(),
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = currentScenarioState,
    status: Int = 200,
  ) {
    val matchIdUrlPattern: UrlPattern = matchId?.let { urlEqualTo("/person/score/$it") } ?: urlPathMatching("/person/score/.*") // Regex to match any matchId, as not known on create
    authSetup()
    stubGetRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      urlPattern = matchIdUrlPattern,
      status = status,
      body = objectMapper.writeValueAsString(personMatchResponse),
    )
  }

  private fun stubIsClusterValid(isClusterValidResponse: IsClusterValidResponse, scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED, status: Int = 200) {
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      url = "/is-cluster-valid",
      status = status,
      responseBody = objectMapper.writeValueAsString(isClusterValidResponse),
    )
  }

  internal fun stubClusterIsValid(scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) = stubIsClusterValid(isClusterValidResponse = IsClusterValidResponse(isClusterValid = true, clusters = listOf()), scenario, currentScenarioState, nextScenarioState)

  internal fun stubClusterIsNotValid(clusters: List<ValidCluster> = listOf()) = stubIsClusterValid(isClusterValidResponse = IsClusterValidResponse(isClusterValid = false, clusters = clusters.map { cluster -> cluster.records }))

  internal fun stubPersonMatchUpsert(
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = currentScenarioState,
    status: Int = 200,
    body: String = "{}",
  ) {
    authSetup()
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      url = "/person",
      status = status,
      responseBody = body,
    )
  }

  internal fun stubDeletePersonMatch(status: Int = 200, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED) {
    stubDeleteRequest(
      url = "/person",
      status = status,
      currentScenarioState = currentScenarioState,
      nextScenarioState = nextScenarioState,
    )
  }

  fun stub404Response(url: String) {
    authSetup()
    wiremock.stubFor(
      WireMock.get(url)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetRequestWithTimeout(url: String, currentScenarioState: String, nextScenarioState: String) {
    authSetup()
    wiremock.stubFor(
      WireMock.get(url)
        .inScenario(BASE_SCENARIO)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withFixedDelay(210),
        ),
    )
  }

  internal fun stubGetRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, url: String, body: String, status: Int = 200) {
    stubGetRequest(scenarioName, currentScenarioState, nextScenarioState, urlEqualTo(url), body, status)
  }

  internal fun stubGetRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, urlPattern: UrlPattern, body: String, status: Int = 200) {
    authSetup()
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

  internal fun stubPostRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, url: String, responseBody: String, status: Int = 200) {
    authSetup()
    wiremock.stubFor(
      WireMock.post(url)
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(responseBody),
        ),
    )
  }

  internal fun stubPostRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, url: String, responseBody: String, status: Int = 200, requestBody: String, fixedDelay: Int = 0) {
    authSetup()
    wiremock.stubFor(
      WireMock.post(url)
        .withRequestBody(equalToJson(requestBody))
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(responseBody)
            .withFixedDelay(fixedDelay),
        ),
    )
  }

  internal fun stubDeleteRequest(scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED, url: String, body: String = "{}", status: Int = 200) {
    authSetup()
    wiremock.stubFor(
      WireMock.delete(url)
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

  internal fun stub5xxResponse(url: String, nextScenarioState: String = "Next request will succeed", scenarioName: String? = BASE_SCENARIO, currentScenarioState: String = STARTED, status: Int = 500) = stubGetRequest(scenarioName, currentScenarioState, nextScenarioState, url, body = "", status = status)

  internal fun stubPrisonResponse(
    apiResponseSetup: ApiResponseSetup,
    scenarioName: String? = BASE_SCENARIO,
    currentScenarioState: String? = STARTED,
    nextScenarioState: String? = currentScenarioState,
  ) = stubGetRequest(scenarioName, currentScenarioState, nextScenarioState, "/prisoner/${apiResponseSetup.prisonNumber}", prisonerSearchResponse(apiResponseSetup))

  internal fun stubSingleProbationResponse(probationCase: ApiResponseSetup, scenarioName: String, currentScenarioState: String, nextScenarioState: String) = stubGetRequest(scenarioName, currentScenarioState, nextScenarioState, "/probation-cases/${probationCase.crn}", probationCaseResponse(probationCase))

  internal fun blitz(actionCount: Int, threadCount: Int, action: () -> Unit) {
    val blitzer = Blitzer(actionCount, threadCount)
    try {
      blitzer.blitz {
        action()
      }
    } finally {
      blitzer.shutdown()
    }
  }

  internal fun PersonKeyEntity.assertClusterIsOfSize(size: Int) = awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)?.personEntities?.size).isEqualTo(size) }

  internal fun PersonKeyEntity.assertClusterStatus(status: UUIDStatusType) = awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)?.status).isEqualTo(status) }

  internal fun PersonKeyEntity.assertMergedTo(mergedCluster: PersonKeyEntity) {
    awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)?.mergedTo).isEqualTo(mergedCluster.id) }
  }
  internal fun PersonKeyEntity.assertNotMergedTo(mergedCluster: PersonKeyEntity) {
    awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)?.mergedTo).isNotEqualTo(mergedCluster.id) }
  }

  internal fun PersonEntity.assertNotLinkedToCluster() {
    awaitAssert { assertThat(personRepository.findByMatchId(this.matchId)?.personKey).isNull() }
  }

  internal fun PersonEntity.assertMergedTo(mergedRecord: PersonEntity) {
    awaitAssert { assertThat(personRepository.findByMatchId(this.matchId)?.mergedTo).isEqualTo(mergedRecord.id) }
  }

  internal fun PersonEntity.assertNotMerged() {
    awaitAssert { assertThat(personRepository.findByMatchId(this.matchId)?.mergedTo).isNull() }
  }

  internal fun PersonEntity.assertNotLinkedToCluster(cluster: PersonKeyEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.personKey?.personUUID).isNotEqualTo(cluster.personUUID)
  }

  internal fun PersonEntity.assertLinkedToCluster(cluster: PersonKeyEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.personKey?.personUUID).isEqualTo(cluster.personUUID)
  }

  internal fun PersonEntity.assertHasLinkToCluster() = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.personKey).isNotNull()
  }

  internal fun PersonEntity.assertExcludedFrom(personEntity: PersonEntity) = awaitAssert {
    assertThat(
      personRepository.findByMatchId(this.matchId)?.overrideMarkers?.filter { it.markerType == EXCLUDE && it.markerValue == personEntity.id },
    ).hasSize(1)
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
