package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.ContactInfo
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.DemographicAttributes
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Name
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconSyncIntTest : WebTestBase() {

  @Test
  fun `can send PUT`() {
    val prisoner = createExampleSysconPrisoner()
    webTestClient
      .put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .body(Mono.just(prisoner), Prisoner::class.java)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val prisoner = createExampleSysconPrisoner()
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .body(Mono.just(prisoner), Prisoner::class.java)
      .authorised(listOf("UNSUPPORTED-ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
      .expectBody()
      .jsonPath("userMessage")
      .isEqualTo(expectedErrorMessage)
  }

  @Test
  fun `should return UNAUTHORIZED 401 when role is not set`() {
    val prisoner = createExampleSysconPrisoner()
    webTestClient.put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .body(Mono.just(prisoner), Prisoner::class.java)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun createExampleSysconPrisoner(): Prisoner = Prisoner(
    name = Name(
      titleCode = "MR",
      firstName = randomName(),
      middleNames = randomName(),
      lastName = randomName(),
    ),
    demographicAttributes = DemographicAttributes(
      dateOfBirth = randomDate(),
      birthPlace = "",
      birthCountryCode = "",
      ethnicityCode = randomPrisonEthnicity(),
      sexCode = "",
    ),
    addresses = emptyList(),
    religions = emptyList(),
    contactInfo = ContactInfo(
      phoneNumbers = emptyList(),
      emails = emptyList(),
    ),
    aliases = emptyList(),
    identifiers = emptyList(),
    sentences = emptyList(),
  )
}
