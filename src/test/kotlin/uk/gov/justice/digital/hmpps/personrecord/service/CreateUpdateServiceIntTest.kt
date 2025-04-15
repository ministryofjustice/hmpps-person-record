package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class CreateUpdateServiceIntTest: IntegrationTestBase() {

  @Autowired
  lateinit var personService: PersonService

  private val reclusterService = mock<ReclusterService>()

  @Test
  fun `should not recluster when no change in matching fields`() {
    val createUpdateService = CreateUpdateService(personService, reclusterService)

    val person = createRandomProbationPersonDetails()
    val existingPerson = createPersonWithNewKey(person)

    stubPersonMatchUpsert()
    createUpdateService.processPerson(person) { existingPerson }

    verifyNoInteractions(reclusterService)
  }

  @Test
  fun `should recluster when change in matching fields`() {
    val createUpdateService = CreateUpdateService(personService, reclusterService)

    val person = createRandomProbationPersonDetails()
    val existingPerson = createPersonWithNewKey(person)

    stubPersonMatchUpsert()
    createUpdateService.processPerson(person) { existingPerson }

    verifyNoInteractions(reclusterService)
  }
}