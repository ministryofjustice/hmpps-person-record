package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
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

  override fun createPerson(person: Person, personKeyEntity: PersonKeyEntity?): PersonEntity {
    val personEntity = super.createPerson(person, personKeyEntity)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  internal fun createProbationPersonFrom(from: Person, crn: String = randomCrn()): Person = from.copy(crn = crn)

  /*
  Remove matching fields to reduce match weight below the join threshold but keep above fracture threshold
   */
  internal fun Person.aboveFracture(): Person = this.copy(
    references = this.references.filterNot { it.identifierType == IdentifierType.PNC || it.identifierType == IdentifierType.CRO },
    sentences = emptyList(),
  )

  internal fun Person.withChangedMatchDetails(): Person = this.copy(
    sentences = this.sentences + SentenceInfo(
      randomDate(),
    ),
  )
}
