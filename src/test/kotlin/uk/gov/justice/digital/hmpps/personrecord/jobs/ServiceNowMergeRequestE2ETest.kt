package uk.gov.justice.digital.hmpps.personrecord.jobs

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow.ServiceNowMergeRequestRepository
import uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow.ServiceNowMergeRequestService.Companion.HOURS_TO_CHOOSE_FROM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private const val GENERATE_MERGE_REQUESTS = "/jobs/service-now/generate-delius-merge-requests"
class ServiceNowMergeRequestE2ETest : E2ETestBase() {

  @Value($$"${service-now.sysparm-id}")
  lateinit var sysParmId: String

  @Value($$"${service-now.requestor}")
  lateinit var requestor: String

  @Value($$"${service-now.requested-for}")
  lateinit var requestedFor: String

  @Autowired
  lateinit var serviceNowMergeRequestRepository: ServiceNowMergeRequestRepository

  var serviceNowStub: StubMapping? = null

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    serviceNowStub = wiremock.stubFor(
      WireMock.post(
        "/api/sn_sc/servicecatalog/items/$sysParmId/order_now",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """{
        "result": {
          "sys_id": "20d3ba6b47a272106322862c736d437c",
          "number": "REQ2039412",
          "request_number": "REQ2039412",
          "parent_id": null,
          "request_id": "20d3ba6b47a272106322862c736d437c",
          "parent_table": "task",
          "table": "sc_request"
        }
      } 
              """.trimIndent(),
            ),
        ),
    )

    serviceNowAuthSetup()
  }

  @AfterEach
  fun afterEach() {
    wiremock.removeStub(serviceNowStub?.uuid)
  }

  fun serviceNowAuthSetup() {
    wiremock.stubFor(
      WireMock.post("/oauth_token.do")
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

  @Test
  fun `should pick ten clusters where each cluster has more than one probation record`() {
    serviceNowMergeRequestRepository.deleteAll()
    val person1 = createRandomProbationPersonDetails()
    val person2 = person1.copy(crn = randomCrn())
    val person3 = createRandomProbationPersonDetails()
    val person4 = person3.copy(crn = randomCrn())
    val person5 = createRandomProbationPersonDetails()
    val person6 = person5.copy(crn = randomCrn())
    val person7 = createRandomProbationPersonDetails()
    val person8 = person7.copy(crn = randomCrn())
    val person9 = createRandomProbationPersonDetails()
    val person10 = person9.copy(crn = randomCrn())
    val person11 = createRandomProbationPersonDetails()
    val person12 = person11.copy(crn = randomCrn())
    val person13 = createRandomProbationPersonDetails()
    val person14 = person13.copy(crn = randomCrn())
    val person15 = createRandomProbationPersonDetails()
    val person16 = person15.copy(crn = randomCrn())
    val person17 = createRandomProbationPersonDetails()
    val person18 = person17.copy(crn = randomCrn())
    val person19 = createRandomProbationPersonDetails()
    val person20 = person19.copy(crn = randomCrn())
    val person21 = createRandomProbationPersonDetails()
    val person22 = person21.copy(crn = randomCrn())

    createPersonKey()
      .addPerson(person1)
      .addPerson(person2)
    createPersonKey()
      .addPerson(person3)
      .addPerson(person4)
    createPersonKey()
      .addPerson(person5)
      .addPerson(person6)
    createPersonKey()
      .addPerson(person7)
      .addPerson(person8)
    createPersonKey()
      .addPerson(person9)
      .addPerson(person10)
    createPersonKey()
      .addPerson(person11)
      .addPerson(person12)
    createPersonKey()
      .addPerson(person13)
      .addPerson(person14)
    createPersonKey()
      .addPerson(person15)
      .addPerson(person16)
    createPersonKey()
      .addPerson(person17)
      .addPerson(person18)
    createPersonKey()
      .addPerson(person19)
      .addPerson(person20)
    createPersonKey()
      .addPerson(person21)
      .addPerson(person22)

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk
    awaitAssert { assertThat(serviceNowMergeRequestRepository.findAll().size).isEqualTo(10) }
    wiremock.verify(10, RequestPatternBuilder.like(serviceNowStub?.request))
  }

  @Test
  fun `should not send a merge request for a cluster which has already had a merge request`() {
    val crn1 = randomCrn()
    val crn2 = randomCrn()
    val tenHoursAgo = LocalDateTime.now().minusHours(HOURS_TO_CHOOSE_FROM)

    val person1 = createRandomProbationPersonDetails(crn1)
    val person2 = createRandomProbationPersonDetails(crn2)
    val cluster = createPersonKey()
      .addPerson(person1)
      .addPerson(person2)

    personRepository.updateLastModifiedDate(crn1, tenHoursAgo.plusMinutes(1))
    personRepository.updateLastModifiedDate(crn2, tenHoursAgo.plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk
    awaitAssert { assertThat(serviceNowMergeRequestRepository.existsByPersonUUID(cluster.personUUID!!)).isTrue() }

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    val sortedCrns = listOf(person1, person2).sortedBy { it.crn }
    val body = """{
          "sysparm_id":"$sysParmId",
          "sysparm_quantity":"1",
          "variables":{
    "requestor":"$requestor",
    "requested_for":"$requestedFor",
    "record_a_details_cpr_ndelius":"[{\"full_name_b\":\"${sortedCrns[0].firstName} ${sortedCrns[0].middleNames} ${sortedCrns[0].lastName}\",\"date_of_birth_b\":\"${sortedCrns[0].dateOfBirth}\",\"case_reference_number_crn_a\":\"${sortedCrns[0].crn}\",\"police_national_computer_pnc_reference_b\":\"${sortedCrns[0].getPnc()}\"},{\"full_name_b\":\"${sortedCrns[1].firstName} ${sortedCrns[1].middleNames} ${sortedCrns[1].lastName}\",\"date_of_birth_b\":\"${sortedCrns[1].dateOfBirth}\",\"case_reference_number_crn_a\":\"${sortedCrns[1].crn}\",\"police_national_computer_pnc_reference_b\":\"${sortedCrns[1].getPnc()}\"}]"
  }
      }"""
    awaitAssert {
      wiremock.verify(
        1,
        RequestPatternBuilder.like(serviceNowStub?.request).withRequestBody(
          equalToJson(
            body,
          ),
        ),
      )
    }
  }

  @Test
  fun `should not send a merge request for a cluster which has two prison records but no probation records`() {
    val prisonNumber1 = randomPrisonNumber()
    val prisonNumber2 = randomPrisonNumber()
    val tenHoursAgo = LocalDateTime.now().minusHours(HOURS_TO_CHOOSE_FROM)
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
      .addPerson(createRandomPrisonPersonDetails(prisonNumber2))

    personRepository.updatePrisonerLastModifiedDate(prisonNumber1, tenHoursAgo.plusMinutes(1))
    personRepository.updatePrisonerLastModifiedDate(prisonNumber2, tenHoursAgo.plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    wiremock.verify(0, RequestPatternBuilder.like(serviceNowStub?.request))
  }

  @Test
  fun `should ignore merged records`() {
    val person1 = createPerson(createRandomProbationPersonDetails())
    val person2 = createPerson(createRandomProbationPersonDetails())
    createPersonKey()
      .addPerson(person1)
    createPersonKey()
      .addPerson(person2)

    val person3 = createRandomProbationPersonDetails()
    val person4 = createRandomProbationPersonDetails()
    createPersonKey()
      .addPerson(person3)
      .addPerson(person4)
    probationMergeEventAndResponseSetup(OFFENDER_MERGED, person1.crn!!, person2.crn!!)

    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_SOURCE_SYSTEM_ID" to person2.crn,
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )
    val tenHoursAgo = LocalDateTime.now().minusHours(HOURS_TO_CHOOSE_FROM)

    personRepository.updateLastModifiedDate(person1.crn!!, tenHoursAgo.plusMinutes(1))
    personRepository.updateLastModifiedDate(person2.crn!!, tenHoursAgo.plusMinutes(2))
    personRepository.updateLastModifiedDate(person3.crn!!, tenHoursAgo.plusMinutes(2))
    personRepository.updateLastModifiedDate(person4.crn!!, tenHoursAgo.plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert { wiremock.verify(1, RequestPatternBuilder.like(serviceNowStub?.request)) }
  }

  @Test
  fun `should ignore records in NEEDS_ATTENTION`() {
    val person1 = createPerson(createRandomProbationPersonDetails())
    val person2 = createPerson(createRandomProbationPersonDetails())
    createPersonKey()
      .addPerson(person1)
      .addPerson(person2)

    val person3 = createRandomProbationPersonDetails()
    val person4 = createRandomProbationPersonDetails()
    createPersonKey(NEEDS_ATTENTION)
      .addPerson(person3)
      .addPerson(person4)

    val tenHoursAgo = LocalDateTime.now().minusHours(HOURS_TO_CHOOSE_FROM)

    personRepository.updateLastModifiedDate(person1.crn!!, tenHoursAgo.plusMinutes(1))
    personRepository.updateLastModifiedDate(person2.crn!!, tenHoursAgo.plusMinutes(2))
    personRepository.updateLastModifiedDate(person3.crn!!, tenHoursAgo.plusMinutes(2))
    personRepository.updateLastModifiedDate(person4.crn!!, tenHoursAgo.plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert { wiremock.verify(1, RequestPatternBuilder.like(serviceNowStub?.request)) }
  }

  private fun PersonRepository.updateLastModifiedDate(crn: String, lastModified: LocalDateTime) {
    val personEntity = findByCrn(crn)!!
    personEntity.lastModified = lastModified
    saveAndFlush(personEntity)
  }

  private fun PersonRepository.updatePrisonerLastModifiedDate(prisonNumber: String, lastModified: LocalDateTime) {
    val personEntity = findByPrisonNumber(prisonNumber)!!
    personEntity.lastModified = lastModified
    saveAndFlush(personEntity)
  }
}
