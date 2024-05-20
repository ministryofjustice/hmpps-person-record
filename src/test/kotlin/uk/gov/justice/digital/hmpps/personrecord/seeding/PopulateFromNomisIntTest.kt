package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.Ignore

class PopulateFromNomisIntTest : WebTestBase() {

  @Test
  fun `populate from nomis`() {
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull  {
      personRepository.findByPrisonNumber("1")
    }

    val prisoner1 = personRepository.findByPrisonNumber("1")!!
    assertThat(prisoner1.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoner1.middleNames).isEqualTo("PrisonerOneMiddleNameOne PrisonerOneMiddleNameTwo")
    assertThat(prisoner1.lastName).isEqualTo("PrisonerOneLastName")
    assertThat(prisoner1.pnc).isEqualTo(PNCIdentifier.from("2012/394773H"))
    assertThat(prisoner1.cro).isEqualTo(CROIdentifier.from("29906/12J"))
    assertThat(prisoner1.dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
    assertThat(prisoner1.aliases[0].firstName).isEqualTo("PrisonerOneAliasOneFirstName")
    assertThat(prisoner1.aliases[0].middleNames).isEqualTo("PrisonerOneAliasOneMiddleNameOne PrisonerOneAliasOneMiddleNameTwo")
    assertThat(prisoner1.aliases[0].lastName).isEqualTo("PrisonerOneAliasOneLastName")
    assertThat(prisoner1.aliases[1].firstName).isEqualTo("PrisonerOneAliasTwoFirstName")
    assertThat(prisoner1.aliases[1].middleNames).isEqualTo("PrisonerOneAliasTwoMiddleNameOne PrisonerOneAliasTwoMiddleNameTwo")
    assertThat(prisoner1.aliases[1].lastName).isEqualTo("PrisonerOneAliasTwoLastName")
    assertThat(prisoner1.sourceSystem).isEqualTo(NOMIS)

    val prisoner2 = personRepository.findByPrisonNumber("2")!!
    assertThat(prisoner2.firstName).isEqualTo("PrisonerTwoFirstName")

    val prisoner3 = personRepository.findByPrisonNumber("3")!!
    assertThat(prisoner3.firstName).isEqualTo("PrisonerThreeFirstName")

    val prisoner4 = personRepository.findByPrisonNumber("4")!!
    assertThat(prisoner4.firstName).isEqualTo("PrisonerFourFirstName")

    val prisoner5 = personRepository.findByPrisonNumber("5")!!
    assertThat(prisoner5.firstName).isEqualTo("PrisonerFiveFirstName")

    val prisoner6 = personRepository.findByPrisonNumber("6")!!
    assertThat(prisoner6.firstName).isEqualTo("PrisonerSixFirstName")

    val prisoner7 = personRepository.findByPrisonNumber("7")!!
    assertThat(prisoner7.firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(prisoner7.middleNames).isEqualTo("")
    assertThat(prisoner7.cro).isEqualTo(CROIdentifier.from(""))
  }

  @Test
  @Ignore
  fun `populate from nomis retries get prisoners`() {
    // first call fails
    oauthSetup.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["prisonerNumberOne","prisonerNumberTwo"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will fail")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
    // second call fails too
    oauthSetup.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["prisonerNumberOne","prisonerNumberTwo"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will fail")
        .willSetStateTo("next request will time out")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503),
        ),
    )

    // third call times out
    oauthSetup.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["prisonerNumberOne","prisonerNumberTwo"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will time out")
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withFixedDelay(500),
        ),
    )

    // Fourth call succeeds
    oauthSetup.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["prisonerNumberOne","prisonerNumberTwo"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody("[{\n  \"prisonerNumber\": \"1\",\n  \"pncNumber\": \"12/394773H\",\n  \"pncNumberCanonicalShort\": \"12/394773H\",\n  \"pncNumberCanonicalLong\": \"2012/394773H\",\n  \"croNumber\": \"29906/12J\",\n  \"bookingId\": \"0001200924\",\n  \"bookNumber\": \"38412A\",\n  \"firstName\": \"PrisonerOneFirstName\",\n  \"middleNames\": \"PrisonerOneMiddleNameOne PrisonerOneMiddleNameTwo\",\n  \"lastName\": \"PrisonerOneLastName\",\n  \"dateOfBirth\": \"1975-04-02\",\n  \"gender\": \"Female\",\n  \"ethnicity\": \"White: Eng./Welsh/Scot./N.Irish/British\",\n  \"youthOffender\": true,\n  \"maritalStatus\": \"Widowed\",\n  \"religion\": \"Church of England (Anglican)\",\n  \"nationality\": \"Egyptian\",\n  \"status\": \"ACTIVE IN\",\n  \"lastMovementTypeCode\": \"CRT\",\n  \"lastMovementReasonCode\": \"CA\",\n  \"inOutStatus\": \"IN\",\n  \"prisonId\": \"MDI\",\n  \"lastPrisonId\": \"MDI\",\n  \"prisonName\": \"HMP Leeds\",\n  \"cellLocation\": \"A-1-002\",\n  \"aliases\": [\n    {\n      \"firstName\": \"PrisonerOneAliasOneFirstName\",\n      \"middleNames\": \"PrisonerOneAliasOneMiddleNameOne PrisonerOneAliasOneMiddleNameTwo\",\n      \"lastName\": \"PrisonerOneAliasOneLastName\",\n      \"dateOfBirth\": \"1975-04-02\",\n      \"gender\": \"Male\",\n      \"ethnicity\": \"White : Irish\"\n    },\n{\n      \"firstName\": \"PrisonerOneAliasTwoFirstName\",\n      \"middleNames\": \"PrisonerOneAliasTwoMiddleNameOne PrisonerOneAliasTwoMiddleNameTwo\",\n      \"lastName\": \"PrisonerOneAliasTwoLastName\",\n      \"dateOfBirth\": \"1975-04-02\",\n      \"gender\": \"Male\",\n      \"ethnicity\": \"White : Irish\"\n    }\n  ],\n  \"alerts\": [\n    {\n      \"alertType\": \"H\",\n      \"alertCode\": \"HA\",\n      \"active\": true,\n      \"expired\": true\n    }\n  ],\n  \"csra\": \"HIGH\",\n  \"category\": \"C\",\n  \"legalStatus\": \"SENTENCED\",\n  \"imprisonmentStatus\": \"LIFE\",\n  \"imprisonmentStatusDescription\": \"Serving Life Imprisonment\",\n  \"mostSeriousOffence\": \"Robbery\",\n  \"recall\": false,\n  \"indeterminateSentence\": true,\n  \"sentenceStartDate\": \"2020-04-03\",\n  \"releaseDate\": \"2023-05-02\",\n  \"confirmedReleaseDate\": \"2023-05-01\",\n  \"sentenceExpiryDate\": \"2023-05-01\",\n  \"licenceExpiryDate\": \"2023-05-01\",\n  \"homeDetentionCurfewEligibilityDate\": \"2023-05-01\",\n  \"homeDetentionCurfewActualDate\": \"2023-05-01\",\n  \"homeDetentionCurfewEndDate\": \"2023-05-02\",\n  \"topupSupervisionStartDate\": \"2023-04-29\",\n  \"topupSupervisionExpiryDate\": \"2023-05-01\",\n  \"additionalDaysAwarded\": 10,\n  \"nonDtoReleaseDate\": \"2023-05-01\",\n  \"nonDtoReleaseDateType\": \"ARD\",\n  \"receptionDate\": \"2023-05-01\",\n  \"paroleEligibilityDate\": \"2023-05-01\",\n  \"automaticReleaseDate\": \"2023-05-01\",\n  \"postRecallReleaseDate\": \"2023-05-01\",\n  \"conditionalReleaseDate\": \"2023-05-01\",\n  \"actualParoleDate\": \"2023-05-01\",\n  \"tariffDate\": \"2023-05-01\",\n  \"releaseOnTemporaryLicenceDate\": \"2023-05-01\",\n  \"locationDescription\": \"Outside - released from Leeds\",\n  \"restrictedPatient\": true,\n  \"supportingPrisonId\": \"LEI\",\n  \"dischargedHospitalId\": \"HAZLWD\",\n  \"dischargedHospitalDescription\": \"Hazelwood House\",\n  \"dischargeDate\": \"2020-05-01\",\n  \"dischargeDetails\": \"Psychiatric Hospital Discharge to Hazelwood House\",\n  \"currentIncentive\": {\n    \"level\": {\n      \"code\": \"STD\",\n      \"description\": \"Standard\"\n    },\n    \"dateTime\": \"2021-07-05T10:35:17\",\n    \"nextReviewDate\": \"2022-11-10\"\n  },\n  \"heightCentimetres\": 200,\n  \"weightKilograms\": 102,\n  \"hairColour\": \"Blonde\",\n  \"rightEyeColour\": \"Green\",\n  \"leftEyeColour\": \"Hazel\",\n  \"facialHair\": \"Clean Shaven\",\n  \"shapeOfFace\": \"Round\",\n  \"build\": \"Muscular\",\n  \"shoeSize\": 10,\n  \"tattoos\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ],\n  \"scars\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ],\n  \"marks\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ]\n},{\n  \"prisonerNumber\": \"2\",\n  \"pncNumber\": \"12/394773H\",\n  \"pncNumberCanonicalShort\": \"12/394773H\",\n  \"pncNumberCanonicalLong\": \"2012/394773H\",\n  \"croNumber\": \"29906/12J\",\n  \"bookingId\": \"0001200924\",\n  \"bookNumber\": \"38412A\",\n  \"firstName\": \"PrisonerTwoFirstName\",\n  \"middleNames\": \"John James\",\n  \"lastName\": \"Larsen\",\n  \"dateOfBirth\": \"1975-04-02\",\n  \"gender\": \"Female\",\n  \"ethnicity\": \"White: Eng./Welsh/Scot./N.Irish/British\",\n  \"youthOffender\": true,\n  \"maritalStatus\": \"Widowed\",\n  \"religion\": \"Church of England (Anglican)\",\n  \"nationality\": \"Egyptian\",\n  \"status\": \"ACTIVE IN\",\n  \"lastMovementTypeCode\": \"CRT\",\n  \"lastMovementReasonCode\": \"CA\",\n  \"inOutStatus\": \"IN\",\n  \"prisonId\": \"MDI\",\n  \"lastPrisonId\": \"MDI\",\n  \"prisonName\": \"HMP Leeds\",\n  \"cellLocation\": \"A-1-002\",\n  \"aliases\": [\n    {\n      \"firstName\": \"Robert\",\n      \"middleNames\": \"Trevor\",\n      \"lastName\": \"Lorsen\",\n      \"dateOfBirth\": \"1975-04-02\",\n      \"gender\": \"Male\",\n      \"ethnicity\": \"White : Irish\"\n    }\n  ],\n  \"alerts\": [\n    {\n      \"alertType\": \"H\",\n      \"alertCode\": \"HA\",\n      \"active\": true,\n      \"expired\": true\n    }\n  ],\n  \"csra\": \"HIGH\",\n  \"category\": \"C\",\n  \"legalStatus\": \"SENTENCED\",\n  \"imprisonmentStatus\": \"LIFE\",\n  \"imprisonmentStatusDescription\": \"Serving Life Imprisonment\",\n  \"mostSeriousOffence\": \"Robbery\",\n  \"recall\": false,\n  \"indeterminateSentence\": true,\n  \"sentenceStartDate\": \"2020-04-03\",\n  \"releaseDate\": \"2023-05-02\",\n  \"confirmedReleaseDate\": \"2023-05-01\",\n  \"sentenceExpiryDate\": \"2023-05-01\",\n  \"licenceExpiryDate\": \"2023-05-01\",\n  \"homeDetentionCurfewEligibilityDate\": \"2023-05-01\",\n  \"homeDetentionCurfewActualDate\": \"2023-05-01\",\n  \"homeDetentionCurfewEndDate\": \"2023-05-02\",\n  \"topupSupervisionStartDate\": \"2023-04-29\",\n  \"topupSupervisionExpiryDate\": \"2023-05-01\",\n  \"additionalDaysAwarded\": 10,\n  \"nonDtoReleaseDate\": \"2023-05-01\",\n  \"nonDtoReleaseDateType\": \"ARD\",\n  \"receptionDate\": \"2023-05-01\",\n  \"paroleEligibilityDate\": \"2023-05-01\",\n  \"automaticReleaseDate\": \"2023-05-01\",\n  \"postRecallReleaseDate\": \"2023-05-01\",\n  \"conditionalReleaseDate\": \"2023-05-01\",\n  \"actualParoleDate\": \"2023-05-01\",\n  \"tariffDate\": \"2023-05-01\",\n  \"releaseOnTemporaryLicenceDate\": \"2023-05-01\",\n  \"locationDescription\": \"Outside - released from Leeds\",\n  \"restrictedPatient\": true,\n  \"supportingPrisonId\": \"LEI\",\n  \"dischargedHospitalId\": \"HAZLWD\",\n  \"dischargedHospitalDescription\": \"Hazelwood House\",\n  \"dischargeDate\": \"2020-05-01\",\n  \"dischargeDetails\": \"Psychiatric Hospital Discharge to Hazelwood House\",\n  \"currentIncentive\": {\n    \"level\": {\n      \"code\": \"STD\",\n      \"description\": \"Standard\"\n    },\n    \"dateTime\": \"2021-07-05T10:35:17\",\n    \"nextReviewDate\": \"2022-11-10\"\n  },\n  \"heightCentimetres\": 200,\n  \"weightKilograms\": 102,\n  \"hairColour\": \"Blonde\",\n  \"rightEyeColour\": \"Green\",\n  \"leftEyeColour\": \"Hazel\",\n  \"facialHair\": \"Clean Shaven\",\n  \"shapeOfFace\": \"Round\",\n  \"build\": \"Muscular\",\n  \"shoeSize\": 10,\n  \"tattoos\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ],\n  \"scars\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ],\n  \"marks\": [\n    {\n      \"bodyPart\": \"Head\",\n      \"comment\": \"Skull and crossbones covering chest\"\n    }\n  ]\n}]"),

        ),
    )

    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(personRepository.findAll().size).isEqualTo(7)
    }

    val prisoners = personRepository.findAll()
    prisoners.sortBy { it.prisonNumber }
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(prisoners[6].aliases.size).isEqualTo(0)
  }

  @Test
  @Ignore
  fun `populate from nomis retries getPrisonerNumbers`() {
    // first call fails
    oauthSetup.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=1&page=1")
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503),
        ),
    )
    // second call succeeds
    oauthSetup.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=1&page=1")
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\n  \"totalPages\": 7,\n  \"totalElements\": 7,\n  \"first\": true,\n  \"last\": false,\n  \"size\": 0,\n  \"content\": [\n    \"prisonerNumberTwo\"\n  ],\n  \"number\": 0,\n  \"sort\": [\n    {\n      \"direction\": \"string\",\n      \"nullHandling\": \"string\",\n      \"ascending\": true,\n      \"property\": \"string\",\n      \"ignoreCase\": true\n    }\n  ],\n  \"numberOfElements\": 0,\n  \"pageable\": {\n    \"offset\": 0,\n    \"sort\": [\n      {\n        \"direction\": \"string\",\n        \"nullHandling\": \"string\",\n        \"ascending\": true,\n        \"property\": \"string\",\n        \"ignoreCase\": true\n      }\n    ],\n    \"pageSize\": 0,\n    \"pageNumber\": 0,\n    \"unpaged\": true,\n    \"paged\": true\n  },\n  \"empty\": true\n}")
            .withStatus(200),

        ),
    )
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(personRepository.findAll().size).isEqualTo(7)
    }
    val prisoners = personRepository.findAll()
    prisoners.sortBy { it.prisonNumber }
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
  }
}
