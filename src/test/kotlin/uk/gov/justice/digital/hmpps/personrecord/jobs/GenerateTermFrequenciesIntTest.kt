package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.util.concurrent.TimeUnit.SECONDS

class GenerateTermFrequenciesIntTest: WebTestBase() {

  @Autowired
  lateinit var pncFrequencyRepository: PncFrequencyRepository

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `should generate and populate pnc term frequency table`() {
    for (i in 0..100) {
      personRepository.saveAndFlush(PersonEntity.from(
        Person.from(Defendant(
          pncId = PNCIdentifier.from(randomPnc()),
          personDefendant = PersonDefendant(
            personDetails = PersonDetails(
              firstName = randomName(),
              lastName = randomName(),
              gender = "Male"
            )
          )
        ))
      ))
    }

    webTestClient.post()
      .uri("/generate/termfrequencies")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted  {
      assertThat(pncFrequencyRepository.findAll().size).isGreaterThan(0)
    }
  }
}