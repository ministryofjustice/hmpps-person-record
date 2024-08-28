package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.termfrequency.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.util.concurrent.TimeUnit.SECONDS

@AutoConfigureWebTestClient(timeout = "3600000")
class GenerateTermFrequenciesIntTest : WebTestBase() {

  @Autowired
  lateinit var pncFrequencyRepository: PncFrequencyRepository

  @Test
  fun `should generate and populate pnc term frequency table`() {
    for (i in 0..100) {
      createPerson()
    }

    webTestClient.post()
      .uri("/jobs/generatetermfrequencies")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(pncFrequencyRepository.findAll().size).isGreaterThanOrEqualTo(100)
    }
  }

  @Test
  fun `should generate different pnc frequencies`() {
    val firstPnc = randomPnc()
    createPerson(firstPnc)

    val secondPnc = randomPnc()
    createPerson(secondPnc)
    createPerson(secondPnc)

    webTestClient.post()
      .uri("/jobs/generatetermfrequencies")
      .exchange()
      .expectStatus()
      .isOk

    val firstFrequency = await.atMost(15, SECONDS) untilNotNull { pncFrequencyRepository.findByPnc(firstPnc) }
    val secondFrequency = await.atMost(15, SECONDS) untilNotNull { pncFrequencyRepository.findByPnc(secondPnc) }

    assertThat(firstFrequency.frequency).isLessThan(secondFrequency.frequency)
  }

  private fun createPerson(pnc: String = randomPnc()) {
    personRepository.saveAndFlush(
      PersonEntity.from(
        Person.from(
          Defendant(
            pncId = PNCIdentifier.from(pnc),
            personDefendant = PersonDefendant(
              personDetails = PersonDetails(
                firstName = randomName(),
                lastName = randomName(),
                gender = "Male",
              ),
            ),
          ),
        ),
      ),
    )
  }
}
