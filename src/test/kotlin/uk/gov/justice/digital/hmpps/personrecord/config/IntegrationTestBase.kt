package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
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
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.extensions.getCROs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review.ReviewEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EthnicityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.telemetry.TelemetryTestRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name as LibraName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName as OffenderName

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTestBase {

  @Autowired
  private lateinit var personFactory: PersonFactory

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

  @Autowired
  lateinit var ethnicityCodeRepository: EthnicityCodeRepository

  @Autowired
  private lateinit var overrideService: OverrideService

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  fun authSetup() {
    wiremock.stubFor(
      WireMock.post("/auth/oauth/token")
        .willReturn(
          aResponse()
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

  internal fun createRandomProbationPersonDetails(crn: String = randomCrn()): Person = Person.from(
    ProbationCase(
      name = OffenderName(firstName = randomName(), middleNames = randomName(), lastName = randomName()),
      title = Value(randomTitleCode().key),
      identifiers = Identifiers(crn = crn, pnc = randomLongPnc(), cro = randomCro()),
      addresses = listOf(
        ProbationAddress(postcode = randomPostcode()),
        ProbationAddress(postcode = randomPostcode()),
      ),
      aliases = listOf(
        ProbationCaseAlias(
          ProbationCaseName(
            firstName = randomName(),
            middleNames = randomName(),
            lastName = randomName(),
          ),
          dateOfBirth = randomDate(),
        ),
      ),
      sentences = listOf(Sentences(randomDate())),
    ),
  )

  internal fun createRandomPrisonPersonDetails(prisonNumber: String = randomPrisonNumber()): Person = Person.from(
    Prisoner(prisonNumber = prisonNumber, firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate()),
  )

  internal fun createRandomLibraPersonDetails(cId: String = randomCId()): Person = Person.from(LibraHearingEvent(name = LibraName(firstName = randomName(), lastName = randomName()), cId = cId))

  internal fun createRandomCommonPlatformPersonDetails(defendantId: String = randomDefendantId()): Person = Person.from(
    Defendant(
      id = defendantId,
      masterDefendantId = randomDefendantId(),
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          firstName = randomName(),
          middleName = randomName(),
          lastName = randomName(),
          dateOfBirth = randomDate(),
        ),
      ),
      pncId = PNCIdentifier.from(randomLongPnc()),
      cro = CROIdentifier.from(randomCro()),
    ),
  )

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String?>,
    times: Int = 1,
  ) {
    awaitAssert {
      val allEvents = telemetryRepository.findAllByEvent(event.eventName)
      val matchingEvents = allEvents?.filter { event ->
        expected.entries.map { (k, v) ->
          val jsonObject = JSONObject(event.properties)
          when {
            (jsonObject.has(k)) -> jsonObject.get(k).equals(v)
            else -> false
          }
        }.all { it }
      }
      assertThat(matchingEvents?.size).`as`("Missing data $event $expected and actual data $allEvents").isEqualTo(times)
    }
  }

  internal fun checkEventLogExist(
    sourceSystemId: String,
    event: CPRLogEvents,
  ) {
    checkEventLog(sourceSystemId, event) { logEvents ->
      assertThat(logEvents).`as`("Missing event log $event and actual data $logEvents").hasSize(1)
    }
  }

  internal fun checkEventLog(
    sourceSystemId: String,
    event: CPRLogEvents,
    matchingEvents: (logEvents: List<EventLogEntity>) -> Unit,
  ) {
    awaitAssert {
      matchingEvents(eventLogRepository.findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(event, sourceSystemId) ?: emptyList())
    }
  }

  internal fun awaitAssert(function: () -> Unit) = await atMost (Duration.ofSeconds(12)) untilAsserted function

  internal fun <T> awaitNotNull(function: () -> T?): T = await atMost (Duration.ofSeconds(3)) untilNotNull function

  internal fun createPersonKey(status: UUIDStatusType = ACTIVE, reason: UUIDStatusReasonType? = null): PersonKeyEntity {
    val personKeyEntity = PersonKeyEntity.new()
    personKeyEntity.apply {
      this.status = status
      this.statusReason = reason
    }
    return personKeyRepository.save(personKeyEntity)
  }

  internal fun PersonKeyEntity.addPerson(personEntity: PersonEntity): PersonKeyEntity {
    this.personEntities.add(personEntity)
    personEntity.personKey = this
    return personKeyRepository.save(this)
  }

  internal fun PersonKeyEntity.addPerson(person: Person): PersonKeyEntity = this.addPerson(createPerson(person))

  internal fun createPersonWithNewKey(person: Person, status: UUIDStatusType = ACTIVE, reason: UUIDStatusReasonType? = null): PersonEntity {
    val personEntity = createPerson(person)
    createPersonKey(status, reason).addPerson(personEntity)
    return personRepository.findByMatchId(personEntity.matchId)!!
  }

  internal fun createPerson(person: Person): PersonEntity {
    val personEntity = personFactory.create(person).personEntity
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
    val target = personRepository.findByMatchId(excludingRecord.matchId)
    overrideService.systemExclude(source!!, target!!)
    personRepository.saveAll(listOf(source, target))
  }

  internal fun includeRecords(vararg records: PersonEntity) {
    val updatedRecords = records.mapNotNull { personRepository.findByMatchId(it.matchId) }.toTypedArray()
    overrideService.systemInclude(*updatedRecords)
    personRepository.saveAll(updatedRecords.asList())
  }

  internal fun stubNoMatchesPersonMatch(matchId: UUID? = null) = stubPersonMatchScores(matchId = matchId, personMatchResponse = emptyList())

  internal fun stubOnePersonMatchAboveJoinThreshold(matchId: UUID? = null, matchedRecord: UUID) = stubXPersonMatches(
    matchId = matchId,
    aboveJoin = listOf(matchedRecord),
  )

  internal fun stubOnePersonMatchAboveFractureThreshold(matchId: UUID? = null, matchedRecord: UUID) = stubXPersonMatches(
    matchId = matchId,
    aboveFracture = listOf(matchedRecord),
  )

  internal fun stubOnePersonMatchBelowFractureThreshold(matchId: UUID? = null, matchedRecord: UUID) = stubXPersonMatches(
    matchId = matchId,
    belowFracture = listOf(matchedRecord),
  )

  internal fun stubXPersonMatches(matchId: UUID? = null, aboveJoin: List<UUID> = emptyList(), aboveFracture: List<UUID> = emptyList(), belowFracture: List<UUID> = emptyList()) = stubPersonMatchScores(
    matchId = matchId,
    personMatchResponse = List(aboveJoin.size) { index ->
      PersonMatchScore(
        candidateMatchId = aboveJoin[index].toString(),
        candidateMatchWeight = JOIN_THRESHOLD + 1F,
        candidateMatchProbability = 0.999999F,
        candidateShouldFracture = false,
        candidateShouldJoin = true,
      )
    } + List(aboveFracture.size) { index ->
      PersonMatchScore(
        candidateMatchId = aboveFracture[index].toString(),
        candidateMatchWeight = FRACTURE_THRESHOLD + 1F,
        candidateMatchProbability = 0.999999F,
        candidateShouldFracture = false,
        candidateShouldJoin = false,
      )
    } + List(belowFracture.size) { index ->
      PersonMatchScore(
        candidateMatchId = belowFracture[index].toString(),
        candidateMatchWeight = FRACTURE_THRESHOLD - 1F,
        candidateMatchProbability = 0.999999F,
        candidateShouldFracture = true,
        candidateShouldJoin = false,
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

  internal fun stubClusterIsValid(
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      url = "/is-cluster-valid",
      status = 200,
      responseBody = objectMapper.writeValueAsString(IsClusterValidResponse(isClusterValid = true)),
    )
  }

  internal fun stubPersonMatchUpsert(
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = currentScenarioState,
    status: Int = 200,
  ) {
    authSetup()
    stubPostRequest(
      scenario,
      currentScenarioState,
      nextScenarioState,
      url = "/person",
      status = status,
      responseBody = "{}",
    )
  }

  internal fun stubDeletePersonMatch(status: Int = 200, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED) {
    authSetup()
    wiremock.stubFor(
      WireMock.delete("/person")
        .inScenario(BASE_SCENARIO)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody("{}"),
        ),
    )
  }

  fun stub404Response(url: String) {
    authSetup()
    wiremock.stubFor(
      WireMock.get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
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
          aResponse()
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
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status)
            .withBody(responseBody),
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

  internal fun stubSingleProbationResponse(probationCase: ApiResponseSetup, scenarioName: String? = BASE_SCENARIO, currentScenarioState: String? = STARTED, nextScenarioState: String? = STARTED) = stubGetRequest(scenarioName, currentScenarioState, nextScenarioState, "/probation-cases/${probationCase.crn}", probationCaseResponse(probationCase))

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

  internal fun PersonKeyEntity.getReview(): ReviewEntity = awaitNotNull {
    reviewRepository.findByClustersClusterTypeAndClustersPersonKey(
      ClusterType.PRIMARY,
      this,
    )
  }

  internal fun ReviewEntity.removed() = awaitAssert { assertThat(reviewRepository.findById(this.id!!)).isNotNull() }

  internal fun ReviewEntity.hasReviewSize(size: Int): ReviewEntity {
    assertThat(this.clusters).hasSize(size)
    return this
  }

  internal fun ReviewEntity.isPrimary(personKeyEntity: PersonKeyEntity): ReviewEntity {
    val primary = this.clusters.find { it.clusterType == ClusterType.PRIMARY }
    assertThat(primary?.clusterType).isEqualTo(ClusterType.PRIMARY)
    assertThat(primary?.personKey?.personUUID).isEqualTo(personKeyEntity.personUUID)
    return this
  }

  internal fun ReviewEntity.isAdditional(vararg additionalClusters: PersonKeyEntity): ReviewEntity {
    val additional = this.clusters.filter { it.clusterType == ClusterType.ADDITIONAL }
    assertThat(additional).hasSize(additionalClusters.size)
    assertThat(additional.map { it.personKey.personUUID }.toSet()).isEqualTo(additionalClusters.map { it.personUUID }.toSet())
    return this
  }

  internal fun PersonKeyEntity.assertClusterIsOfSize(size: Int) = awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)?.personEntities?.size).isEqualTo(size) }

  internal fun PersonKeyEntity.assertClusterStatus(status: UUIDStatusType, reason: UUIDStatusReasonType? = null) = awaitAssert {
    val cluster = personKeyRepository.findByPersonUUID(this.personUUID)
    assertThat(cluster?.status).isEqualTo(status)
    assertThat(cluster?.statusReason).isEqualTo(reason)
  }

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

  internal fun PersonEntity.assertHasSameOverrideMarker(personEntity: PersonEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.overrideMarker)
      .isEqualTo(personRepository.findByMatchId(personEntity.matchId)?.overrideMarker)
  }

  internal fun PersonEntity.assertHasOverrideMarker() = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.overrideMarker).isNotNull()
  }

  internal fun PersonEntity.assertDoesNotHaveOverrideMarker() = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.overrideMarker).isNull()
  }

  internal fun PersonEntity.assertHasDifferentOverrideMarker(personEntity: PersonEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.overrideMarker).isNotEqualTo(personEntity.overrideMarker)
  }

  internal fun PersonEntity.assertHasSameOverrideScope(personEntity: PersonEntity) = awaitAssert {
    assertThat(this.intersectScopes(personEntity)).isNotEmpty()
  }

  internal fun PersonEntity.assertHasDifferentOverrideScope(personEntity: PersonEntity) = awaitAssert {
    assertThat(this.intersectScopes(personEntity)).isEmpty()
  }

  internal fun PersonEntity.assertOverrideScopeSize(size: Int) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.overrideScopes?.size).isEqualTo(size)
  }

  internal fun PersonEntity.assertExcluded(personEntity: PersonEntity) {
    this.assertHasDifferentOverrideMarker(personEntity)
    this.assertHasSameOverrideScope(personEntity)
  }

  internal fun PersonEntity.assertIncluded(personEntity: PersonEntity) {
    this.assertHasSameOverrideMarker(personEntity)
    this.assertHasSameOverrideScope(personEntity)
  }

  fun PersonEntity.assertPersonDeleted() = awaitAssert { assertThat(personRepository.findByMatchId(this.matchId)).isNull() }

  fun PersonKeyEntity.assertPersonKeyDeleted() = awaitAssert { assertThat(personKeyRepository.findByPersonUUID(this.personUUID)).isNull() }

  fun Person.getCro(): String? = this.references.getType(CRO).first().identifierValue
  fun PersonEntity.getCro(): String? = this.references.getCROs().firstOrNull()
  fun Person.getPnc(): String? = this.references.getType(PNC).first().identifierValue
  fun PersonEntity.getPnc(): String? = this.references.getPNCs().firstOrNull()

  private fun PersonEntity.intersectScopes(personEntity: PersonEntity): Set<UUID> {
    val thisPersonScopes = personRepository.findByMatchId(this.matchId)?.overrideScopes?.map { it.scope }?.toSet() ?: emptySet()
    val evalPersonScopes = personRepository.findByMatchId(personEntity.matchId)?.overrideScopes?.map { it.scope }?.toSet() ?: emptySet()
    return thisPersonScopes.intersect(evalPersonScopes)
  }

  companion object {

    internal const val BASE_SCENARIO = "baseScenario"

    @JvmStatic
    @RegisterExtension
    var wiremock: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .failOnUnmatchedRequests(true)
      .build()

    internal const val JOIN_THRESHOLD = 24F
    internal const val FRACTURE_THRESHOLD = 18F
  }
}
