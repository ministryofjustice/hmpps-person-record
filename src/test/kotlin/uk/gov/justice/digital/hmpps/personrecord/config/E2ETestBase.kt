package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ActiveProfiles("e2e")
class E2ETestBase : MessagingTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  override fun createPerson(person: Person): PersonEntity {
    val personEntity = super.createPerson(person)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  internal fun createProbationPersonFrom(from: Person, crn: String = randomCrn()): Person = from.copy(crn = crn)

  internal fun createProbationPersonFrom(probationCase: ProbationCase, crn: String = randomCrn()): Person = Person.from(probationCase).copy(crn = crn)

  /*
  Remove matching fields to reduce match weight below the join threshold but keep above fracture threshold
   */
  internal fun Person.aboveFracture(): Person = this.copy(
    references = this.references.filterNot { it.identifierType == IdentifierType.PNC || it.identifierType == IdentifierType.CRO },
    sentences = emptyList(),
  )

  internal fun ProbationCase.aboveFracture(): ProbationCase = this.copy(
    identifiers = this.identifiers.copy(pnc = null, cro = null),
    sentences = emptyList(),
  )

  internal fun ProbationCase.withChangedMatchDetails(): ProbationCase = this.copy(
    addresses = this.addresses + ProbationAddress(postcode = randomPostcode()),
  )
}
