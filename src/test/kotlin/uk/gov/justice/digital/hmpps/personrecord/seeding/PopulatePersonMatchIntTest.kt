package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

@ActiveProfiles("seeding")
class PopulatePersonMatchIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAllInBatch()
  }

  @Test
  fun `populate person match`() {
    // Populate person data
    blitz(1000, 10) {
      createPersonWithNewKey(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      )
    }

    webTestClient.post()
      .uri("/populatepersonmatch")
      .exchange()
      .expectStatus()
      .isOk
  }
}
