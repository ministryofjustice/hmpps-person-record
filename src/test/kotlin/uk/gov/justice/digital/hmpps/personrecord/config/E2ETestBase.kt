package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ActiveProfiles("e2e")
@AutoConfigureWebTestClient
@ExtendWith(RetryTestExtension::class)
class E2ETestBase : MessagingTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  override fun createPerson(person: Person, configure: PersonEntity.() -> Unit): PersonEntity {
    val personEntity = super.createPerson(person, configure)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  internal fun createProbationPerson(probationCase: ProbationCase = createRandomProbationCase()): PersonEntity = createPerson(Person.from(probationCase))
  internal fun createMatchingRecord(probationCase: ProbationCase): PersonEntity = createPerson(Person.from(probationCase).copy(crn = randomCrn()))

  /*
  Remove matching fields to reduce match weight below the join threshold but keep above fracture threshold
   */
  internal fun ProbationCase.aboveFracture(): ProbationCase = this.copy(
    name = name.copy(firstName = randomName(), middleNames = randomName()),
    identifiers = this.identifiers.copy(cro = null),
    sentences = emptyList(),
    dateOfBirth = randomDate(),
  )

  internal fun ProbationCase.withChangedMatchDetails(): ProbationCase = this.copy(
    sentences = this.sentences?.plus(Sentences(randomDate())),
  )

  protected final inline fun <reified T : Any> sendPostRequestAsserted(
    url: String,
    body: Any,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> = sendRequestAsserted(url, body, roles, expectedStatus, sendAuthorised, HttpMethod.POST)

  protected final inline fun <reified T : Any> sendRequestAsserted(
    url: String,
    body: Any?,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
    methodType: HttpMethod,
  ): WebTestClient.BodySpec<T, *> {
    val requestSpec = webTestClient
      .method(methodType)
      .uri(url)
      .contentType(MediaType.APPLICATION_JSON)

    val requestSpecReady = when (methodType) {
      HttpMethod.GET, HttpMethod.DELETE -> requestSpec
      else -> requestSpec.bodyValue(body!!)
    }

    val responseSpec = when (sendAuthorised) {
      true -> requestSpecReady.authorised(roles).exchange()
      false -> requestSpecReady.exchange()
    }.expectStatus().isEqualTo(expectedStatus.value())
    return responseSpec.expectBody<T>()
  }
}
