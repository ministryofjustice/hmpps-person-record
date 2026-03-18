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
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
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
    personKeyRepository.deleteAll()
    personRepository.deleteAll()
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
  fun `should pick five clusters where each cluster has more than one probation record`() {
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

    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
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
    val fifthPerson = createPersonKey()
      .addPerson(person9)
      .addPerson(person10)
    createPersonKey()
      .addPerson(person11)
      .addPerson(person12)

    personRepository.updateLastModifiedDate(person1.crn!!, thisTimeYesterday.plusMinutes(1))
    personRepository.updateLastModifiedDate(person2.crn!!, thisTimeYesterday.plusMinutes(2))
    personRepository.updateLastModifiedDate(person3.crn!!, thisTimeYesterday.plusMinutes(3))
    personRepository.updateLastModifiedDate(person4.crn!!, thisTimeYesterday.plusMinutes(4))
    personRepository.updateLastModifiedDate(person5.crn!!, thisTimeYesterday.plusMinutes(5))
    personRepository.updateLastModifiedDate(person6.crn!!, thisTimeYesterday.plusMinutes(6))
    personRepository.updateLastModifiedDate(person7.crn!!, thisTimeYesterday.plusMinutes(7))
    personRepository.updateLastModifiedDate(person8.crn!!, thisTimeYesterday.plusMinutes(8))
    personRepository.updateLastModifiedDate(person9.crn!!, thisTimeYesterday.plusMinutes(9))
    personRepository.updateLastModifiedDate(person10.crn!!, thisTimeYesterday.plusMinutes(10))
    personRepository.updateLastModifiedDate(person11.crn!!, thisTimeYesterday.plusMinutes(11))
    personRepository.updateLastModifiedDate(person12.crn!!, thisTimeYesterday.plusMinutes(12))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk
    awaitAssert { assertThat(serviceNowMergeRequestRepository.existsByPersonUUID(fifthPerson.personUUID!!)).isTrue() }
    wiremock.verify(5, RequestPatternBuilder.like(serviceNowStub?.request))
  }

  @Test
  fun `should not send a merge request for a cluster which has already had a merge request`() {
    val crn1 = randomCrn()
    val crn2 = randomCrn()
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)

    val person1 = createRandomProbationPersonDetails(crn1)
    val person2 = createRandomProbationPersonDetails(crn2)
    val cluster = createPersonKey()
      .addPerson(person1)
      .addPerson(person2)

    personRepository.updateLastModifiedDate(crn1, thisTimeYesterday.plusMinutes(1))
    personRepository.updateLastModifiedDate(crn2, thisTimeYesterday.plusMinutes(2))

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
    "[ {\n" +
      "      \"full_name_b\":\"${person1.firstName} \${person1.middleNames} \${person1.lastName}\",\n" +
      "      \"date_of_birth_b\":\"${person1.dateOfBirth}\",\n" +
      "      \"case_reference_number_crn_a\":\"$crn1\",\n" +
      "      \"police_national_computer_pnc_reference_b\":\"${person1.getPnc()}\"\n" +
      "    }, {\n" +
      "      \"full_name_b\":\"${person2.firstName} \${person2.middleNames} \${person2.lastName}\",\n" +
      "      \"date_of_birth_b\":\"${person2.dateOfBirth}\",\n" +
      "      \"case_reference_number_crn_a\":\"$crn2\",\n" +
      "      \"police_national_computer_pnc_reference_b\":\"${person2.getPnc()}\"\n" +
      "    } ]"
    val body = """{
          "sysparm_id":"$sysParmId",
          "sysparm_quantity":"1",
          "variables":{
    "requestor":"$requestor",
    "requested_for":"$requestedFor",
    "record_a_details_cpr_ndelius":"[{\"full_name_b\":\"${person1.firstName} ${person1.middleNames} ${person1.lastName}\",\"date_of_birth_b\":\"${person1.dateOfBirth}\",\"case_reference_number_crn_a\":\"$crn1\",\"police_national_computer_pnc_reference_b\":\"${person1.getPnc()}\"},{\"full_name_b\":\"${person2.firstName} ${person2.middleNames} ${person2.lastName}\",\"date_of_birth_b\":\"${person2.dateOfBirth}\",\"case_reference_number_crn_a\":\"$crn2\",\"police_national_computer_pnc_reference_b\":\"${person2.getPnc()}\"}]"
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
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
      .addPerson(createRandomPrisonPersonDetails(prisonNumber2))

    personRepository.updatePrisonerLastModifiedDate(prisonNumber1, thisTimeYesterday.plusMinutes(1))
    personRepository.updatePrisonerLastModifiedDate(prisonNumber2, thisTimeYesterday.plusMinutes(2))

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
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)

    personRepository.updateLastModifiedDate(person1.crn!!, thisTimeYesterday.plusMinutes(1))
    personRepository.updateLastModifiedDate(person2.crn!!, thisTimeYesterday.plusMinutes(2))
    personRepository.updateLastModifiedDate(person3.crn!!, thisTimeYesterday.plusMinutes(2))
    personRepository.updateLastModifiedDate(person4.crn!!, thisTimeYesterday.plusMinutes(2))

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
