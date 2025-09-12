package uk.gov.justice.digital.hmpps.personrecord.service

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

class PersonServiceIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var personService: PersonService

  @Test
  fun `should send all matchIds of cluster on create recluster merge`() {
    val personA = createPersonWithNewKey(createRandomProbationPersonDetails())
    val personB = createPersonWithNewKey(createRandomProbationPersonDetails())

    val personC = createRandomProbationPersonDetails()

    stubPersonMatchUpsert()
    stubXPersonMatches(aboveJoin = listOf(personA.matchId, personB.matchId))
    authSetup()
    wiremock.stubFor(
      WireMock.post("/is-cluster-valid")
        .withRequestBody(matchingJsonPath("[?($.size() == 3)]"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(IsClusterValidResponse(isClusterValid = true, clusters = emptyList()))),
        ),
    )

    personService.handlePersonCreation(personC)
  }
}
